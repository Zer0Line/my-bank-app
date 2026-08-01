package ru.yandex.practicum.accounts.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.accounts.client.NotificationServiceClient;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.OperationRequest;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.entity.AccountEntity;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountsService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final NotificationServiceClient notificationClient;

    @Transactional(readOnly = true)
    public AccountResponse getAccount() {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);
        log.info("Account found for login='{}': {}", login, account);
        return buildFullResponse(login, account);
    }

    @CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackAfterNotification")
    @Retry(name = "notification-service")
    public AccountResponse updateAccount(UpdateAccountRequest request) {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);

        String[] parts = request.name().split(" ", 2);
        account.setSurename(parts[0]);
        account.setName(parts.length > 1 ? parts[1] : "");
        account.setDateOfBirth(LocalDate.parse(request.birthdate()));

        AccountEntity saved = accountRepository.save(account);
        log.info("Account updated for login='{}': {}", login, saved);
        notificationClient.saveOperation(new OperationRequest(login, "ACCOUNT_UPDATE", "Account details updated", null));
        return buildFullResponse(login, saved);
    }

    @CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackAfterNotification")
    @Retry(name = "notification-service")
    public AccountResponse updateAmount(UpdateAmountRequest request) {
        AccountEntity account = findAccountByLogin(request.login());

        BigDecimal newAmount = switch (request.action()) {
            case GET -> account.getAmount().subtract(request.value());
            case PUT -> account.getAmount().add(request.value());
        };

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        account.setAmount(newAmount);
        AccountEntity saved = accountRepository.save(account);
        log.info("Amount updated for login='{}': new amount={}", request.login(), newAmount);
        notificationClient.saveOperation(new OperationRequest(request.login(), "CASH_" + request.action().name(), "Cash operation", request.value()));
        return buildFullResponse(request.login(), saved);
    }

    @CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackAfterNotificationBatch")
    @Retry(name = "notification-service")
    public AccountResponse transfer(TransferRequest request) {
        AccountEntity sender = findAccountByLogin(request.senderLogin());
        AccountEntity recipient = findAccountByLogin(request.recipientLogin());

        BigDecimal newSenderAmount = sender.getAmount().subtract(request.amount());
        if (newSenderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        sender.setAmount(newSenderAmount);
        recipient.setAmount(recipient.getAmount().add(request.amount()));

        accountRepository.save(sender);
        accountRepository.save(recipient);

        log.info("Transfer completed: sender='{}', recipient='{}', amount={}",
                request.senderLogin(), request.recipientLogin(), request.amount());

        notificationClient.saveOperations(List.of(
                new OperationRequest(request.senderLogin(), "TRANSFER_SENT",
                        "Transfer to " + request.recipientLogin(), request.amount()),
                new OperationRequest(request.recipientLogin(), "TRANSFER_RECEIVED",
                        "Transfer from " + request.senderLogin(), request.amount())
        ));

        return buildFullResponse(request.senderLogin(), sender);
    }

    private AccountResponse fallbackAfterNotification(UpdateAccountRequest request, Throwable t) {
        log.warn("Notification service unavailable for updateAccount, proceeding without notification", t);
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);
        return buildFullResponse(login, account);
    }

    private AccountResponse fallbackAfterNotification(UpdateAmountRequest request, Throwable t) {
        log.warn("Notification service unavailable for updateAmount, proceeding without notification", t);
        AccountEntity account = findAccountByLogin(request.login());
        return buildFullResponse(request.login(), account);
    }

    private AccountResponse fallbackAfterNotificationBatch(TransferRequest request, Throwable t) {
        log.warn("Notification service unavailable for transfer notification, proceeding without notification", t);
        AccountEntity sender = findAccountByLogin(request.senderLogin());
        return buildFullResponse(request.senderLogin(), sender);
    }

    private AccountResponse buildFullResponse(String login, AccountEntity account) {
        List<AccountEntity> otherAccounts = accountRepository.findAllByLoginIsNot(login);
        List<AccountDto> accountDtos = otherAccounts.stream()
                .map(accountMapper::toDto)
                .toList();
        return new AccountResponse(
                account.getLogin(),
                account.getSurename() + " " + account.getName(),
                account.getDateOfBirth().toString(),
                account.getAmount(),
                accountDtos
        );
    }

    private String extractLogin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String login = jwt.getClaimAsString("preferred_username");
            log.info("Token claims: preferred_username={}, subject={}, issuer={}",
                    login, jwt.getSubject(), jwt.getIssuer());
            return login;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private AccountEntity findAccountByLogin(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + login));
    }
}