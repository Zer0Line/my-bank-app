package ru.yandex.practicum.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.service.NotificationService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void addOperation() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperationRequest("testuser", "CASH_PUT", "Deposit", BigDecimal.valueOf(500)))))
                .andExpect(status().isOk())
                .andDo(print());

        verify(notificationService).saveOperation(org.mockito.ArgumentMatchers.any(OperationRequest.class));
    }

    @Test
    void addOperations() throws Exception {
        var requests = List.of(
                new OperationRequest("user1", "TRANSFER_SENT", "Transfer", BigDecimal.valueOf(200)),
                new OperationRequest("user2", "TRANSFER_RECEIVED", "Received", BigDecimal.valueOf(200))
        );

        mockMvc.perform(post("/api/notifications/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andDo(print());

        verify(notificationService).saveOperations(anyList());
    }
}
