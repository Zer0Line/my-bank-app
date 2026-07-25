package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.client.CashClient;
import ru.yandex.practicum.mybankfront.client.TransferClient;
import ru.yandex.practicum.mybankfront.dto.AccountDto;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.CashActionRequest;
import ru.yandex.practicum.mybankfront.dto.TransferRequest;
import ru.yandex.practicum.mybankfront.dto.UpdateAccountRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;


@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private CashClient cashClient;

    @Mock
    private TransferClient transferClient;

    @InjectMocks
    private MainController controller;

    private AccountResponse dummyResponse() {
        return new AccountResponse(
                "login",
                "Test User",
                "2000-01-01",
                1000,
                List.of(new AccountDto("recipient", "Recipient"))
        );
    }

    @Test
    public void index() throws Exception {
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/account"))
                .andDo(print());
    }

    @Test
    public void getAccount() throws Exception {
        when(accountsClient.getAccount()).thenReturn(dummyResponse());

        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void editAccount() throws Exception {
        when(accountsClient.updateAccount(any(UpdateAccountRequest.class)))
                .thenReturn(dummyResponse());

        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(post("/account")
                        .param("name", "New Name")
                        .param("birthdate", "2026-07-25"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void editCash() throws Exception {
        when(cashClient.editCash(any(CashActionRequest.class)))
                .thenReturn(dummyResponse());

        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(post("/cash")
                        .param("value", "500")
                        .param("action", "PUT"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void transfer() throws Exception {
        when(transferClient.transfer(any(TransferRequest.class)))
                .thenReturn(dummyResponse());

        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(post("/transfer")
                        .param("value", "200")
                        .param("login", "recipient"))
                .andExpect(status().isOk())
                .andDo(print());
    }

}
