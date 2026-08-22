package ru.yandex.practicum.accounts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.service.AccountsService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AccountsControllerTest {

    @Mock
    private AccountsService accountsService;

    @InjectMocks
    private AccountsController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void getAccount() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(accountsService).getAccount();
    }

    @Test
    void updateAccount() throws Exception {
        mockMvc.perform(put("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAccountRequest("New Name", "2000-01-01"))))
                .andExpect(status().isOk())
                .andDo(print());

        verify(accountsService).updateAccount(any(UpdateAccountRequest.class));
    }

    @Test
    void updateAmount() throws Exception {
        mockMvc.perform(patch("/api/accounts/amount")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAmountRequest("testuser", BigDecimal.valueOf(500), CashAction.PUT))))
                .andExpect(status().isOk())
                .andDo(print());

        verify(accountsService).updateAmount(any(UpdateAmountRequest.class), nullable(String.class));
    }

    @Test
    void transfer() throws Exception {
        mockMvc.perform(post("/api/accounts/transfer")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequest("sender", "recipient", BigDecimal.valueOf(200)))))
                .andExpect(status().isOk())
                .andDo(print());

        verify(accountsService).transfer(any(TransferRequest.class), nullable(String.class));
    }
}
