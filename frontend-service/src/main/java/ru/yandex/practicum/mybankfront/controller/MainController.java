package ru.yandex.practicum.mybankfront.controller;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.client.CashClient;
import ru.yandex.practicum.mybankfront.client.TransferClient;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.CashAction;
import ru.yandex.practicum.mybankfront.dto.CashActionRequest;
import ru.yandex.practicum.mybankfront.dto.TransferRequest;
import ru.yandex.practicum.mybankfront.dto.UpdateAccountRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер main.html.
 *
 * Используемая модель для main.html:
 *      model.addAttribute("name", name);
 *      model.addAttribute("birthdate", birthdate.format(DateTimeFormatter.ISO_DATE));
 *      model.addAttribute("sum", sum);
 *      model.addAttribute("accounts", accounts);
 *      model.addAttribute("errors", errors);
 *      model.addAttribute("info", info);
 *
 * Поля модели:
 *      name - Фамилия Имя текущего пользователя, String (обязательное)
 *      birthdate - дата рождения текущего пользователя, String в формате 'YYYY-MM-DD' (обязательное)
 *      sum - сумма на счету текущего пользователя, Integer (обязательное)
 *      accounts - список аккаунтов, которым можно перевести деньги, List<AccountDto> (обязательное)
 *      errors - список ошибок после выполнения действий, List<String> (не обязательное)
 *      info - строка успешности после выполнения действия, String (не обязательное)
 *
 * С примерами использования можно ознакомиться в тестовом классе заглушке AccountStub
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    /**
     * GET /.
     * Редирект на GET /account
     */
    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    /**
     * GET /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для получения данных аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     */
    @GetMapping("/account")
    @CircuitBreaker(name = "accounts-client", fallbackMethod = "fallbackGetAccount")
    @Retry(name = "accounts-client")
    public String getAccount(Model model) {
        AccountResponse response = accountsClient.getAccount();
        fillModel(model, response, null, null);
        return "main";
    }

    /**
     * POST /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для изменения данных текущего пользователя по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Изменяемые данные:
     * 1. name - Фамилия Имя
     * 2. birthdate - дата рождения в формате YYYY-DD-MM
     */
    @PostMapping("/account")
    @CircuitBreaker(name = "accounts-client", fallbackMethod = "fallbackEditAccount")
    @Retry(name = "accounts-client")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        UpdateAccountRequest request = new UpdateAccountRequest(
                name, birthdate.format(DateTimeFormatter.ISO_DATE)
        );
        AccountResponse response = accountsClient.updateAccount(request);
        fillModel(model, response, null, "Данные обновлены");
        return "main";
    }

    /**
     * POST /cash.
     * Что нужно сделать:
     * 1. Сходить в сервис cash через Gateway API для снятия/пополнения счета текущего аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. action - GET (снять), PUT (пополнить)
     */
    @PostMapping("/cash")
    @CircuitBreaker(name = "cash-client", fallbackMethod = "fallbackEditCash")
    @Retry(name = "cash-client")
    public String editCash(
            Model model,
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") CashAction action
            ) {
        CashActionRequest request = new CashActionRequest(value, action);
        try {
            AccountResponse response = cashClient.editCash(request);
            fillModel(model, response, null, action == CashAction.GET
                    ? "Снято %s руб".formatted(value)
                    : "Положено %s руб".formatted(value));
            return "main";
        } catch (FeignException e) {
            if (e.status() == HttpStatus.BAD_REQUEST.value() && e.contentUTF8().contains("Insufficient funds")) {
                log.warn("Insufficient funds for cash operation: value={}, action={}", value, action);
                fillModel(model, accountsClient.getAccount(),
                        List.of("Недостаточно средств на счету"), null);
                return "main";
            }
            throw e;
        }
    }

    /**
     * POST /transfer.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для перевода со счета текущего аккаунта на счет другого аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. login - логин пользователя получателя
     */
    @PostMapping("/transfer")
    @CircuitBreaker(name = "transfer-client", fallbackMethod = "fallbackTransfer")
    @Retry(name = "transfer-client")
    public String transfer(
            Model model,
            @RequestParam("value") BigDecimal value,
            @RequestParam("login") String login
    ) {
        TransferRequest request = new TransferRequest(value, login);
        try {
            AccountResponse response = transferClient.transfer(request);
            fillModel(model, response, null, "Успешно переведено %s руб клиенту %s".formatted(value, login));
            return "main";
        } catch (FeignException e) {
            if (e.status() == HttpStatus.BAD_REQUEST.value() && e.contentUTF8().contains("Insufficient funds")) {
                log.warn("Insufficient funds for transfer: value={}, to='{}'", value, login);
                fillModel(model, accountsClient.getAccount(),
                        List.of("Недостаточно средств для перевода"), null);
                return "main";
            }
            throw e;
        }
    }

    private String fallbackGetAccount(Throwable t) {
        log.error("Accounts service unavailable", t);
        return "redirect:/account?error=service_unavailable";
    }

    private String fallbackEditAccount(Model model, String name, java.time.LocalDate birthdate, Throwable t) {
        log.error("Accounts service unavailable for update: name='{}', birthdate={}", name, birthdate, t);
        return "redirect:/account?error=update_failed";
    }

    private String fallbackEditCash(Model model, BigDecimal value, CashAction action, Throwable t) {
        log.error("Cash service unavailable: value={}, action={}", value, action, t);
        return "redirect:/account?error=cash_failed";
    }

    private String fallbackTransfer(Model model, BigDecimal value, String login, Throwable t) {
        log.error("Transfer service unavailable: value={}, to='{}'", value, login, t);
        return "redirect:/account?error=transfer_failed";
    }

    private void fillModel(Model model, AccountResponse response,
                           List<String> errors, String info) {
        model.addAttribute("name", response.name());
        model.addAttribute("birthdate", response.birthdate());
        model.addAttribute("sum", response.sum());
        model.addAttribute("accounts", response.accounts());
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }
}
