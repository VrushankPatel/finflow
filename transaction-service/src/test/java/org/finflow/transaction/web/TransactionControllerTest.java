package org.finflow.transaction.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finflow.transaction.dto.TransactionRequest;
import org.finflow.transaction.dto.TransactionResponse;
import org.finflow.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionRequest validRequest;
    private TransactionResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = new TransactionRequest(
                "idemp-key-123",
                "acc-1",
                "acc-2",
                new BigDecimal("100.00"),
                "USD"
        );

        validResponse = new TransactionResponse(
                UUID.randomUUID(),
                "COMPLETED",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should create transaction successfully")
    void testCreateTransaction_Success() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 200 OK for valid request")
    void testCreateTransaction_ValidRequest() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing idempotency key")
    void testCreateTransaction_MissingIdempotencyKey() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                null,
                "acc-1",
                "acc-2",
                new BigDecimal("100.00"),
                "USD"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing from account")
    void testCreateTransaction_MissingFromAccount() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                "idemp-key-123",
                null,
                "acc-2",
                new BigDecimal("100.00"),
                "USD"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing to account")
    void testCreateTransaction_MissingToAccount() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                "idemp-key-123",
                "acc-1",
                null,
                new BigDecimal("100.00"),
                "USD"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing amount")
    void testCreateTransaction_MissingAmount() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                "idemp-key-123",
                "acc-1",
                "acc-2",
                null,
                "USD"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing currency")
    void testCreateTransaction_MissingCurrency() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                "idemp-key-123",
                "acc-1",
                "acc-2",
                new BigDecimal("100.00"),
                null
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for negative amount")
    void testCreateTransaction_NegativeAmount() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                "idemp-key-123",
                "acc-1",
                "acc-2",
                new BigDecimal("-100.00"),
                "USD"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 415 Unsupported Media Type for non-JSON content")
    void testCreateTransaction_NonJsonContent() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for malformed JSON")
    void testCreateTransaction_MalformedJson() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle null response from service")
    void testCreateTransaction_NullResponse() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(null);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return transaction with COMPLETED status")
    void testCreateTransaction_CompletedStatus() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should return transaction with valid UUID")
    void testCreateTransaction_ValidUuid() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(jsonPath("$.transactionId").isString());
    }

    @Test
    @DisplayName("Should return transaction with timestamp")
    void testCreateTransaction_Timestamp() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}