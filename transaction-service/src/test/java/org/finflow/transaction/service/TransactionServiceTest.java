package org.finflow.transaction.service;

import org.finflow.commons.event.TransactionCompletedEvent;
import org.finflow.transaction.domain.Account;
import org.finflow.transaction.domain.LedgerEntry;
import org.finflow.transaction.dto.TransactionRequest;
import org.finflow.transaction.dto.TransactionResponse;
import org.finflow.transaction.repository.AccountRepository;
import org.finflow.transaction.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private TransactionRequest validRequest;

    @BeforeEach
    void setUp() {
        Currency usd = Currency.getInstance("USD");
        fromAccount = Account.builder()
                .accountId("acc-1")
                .ownerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency(usd)
                .version(1L)
                .build();

        toAccount = Account.builder()
                .accountId("acc-2")
                .ownerName("Jane Smith")
                .balance(new BigDecimal("500.00"))
                .currency(usd)
                .version(1L)
                .build();

        validRequest = new TransactionRequest(
                "idemp-key-123",
                "acc-1",
                "acc-2",
                new BigDecimal("100.00"),
                "USD"
        );
    }

    @Test
    @DisplayName("Should process transaction successfully")
    void testProcessTransaction_Success() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getTimestamp()).isNotNull();

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(ledgerRepository, times(2)).save(any(LedgerEntry.class));
        verify(kafkaTemplate).send(eq("transaction-completed"), anyString(), any(TransactionCompletedEvent.class));
        verify(idempotencyService).updateResponse(eq("idemp-key-123"), anyString());
    }

    @Test
    @DisplayName("Should return cached response for duplicate request")
    void testProcessTransaction_DuplicateRequest() {
        String cachedResponse = "{\"transactionId\":\"cached-id\",\"status\":\"COMPLETED\",\"timestamp\":\"2024-01-01T00:00:00Z\"}";
        when(idempotencyService.checkAndMark("idemp-key-123")).thenReturn(Optional.of(cachedResponse));

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response).isNull();
        verify(accountRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when sender account not found")
    void testProcessTransaction_SenderNotFound() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.processTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sender account not found");

        verify(accountRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when receiver account not found")
    void testProcessTransaction_ReceiverNotFound() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.processTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Receiver account not found");
    }

    @Test
    @DisplayName("Should throw exception when insufficient funds")
    void testProcessTransaction_InsufficientFunds() {
        Account poorAccount = Account.builder()
                .accountId("acc-1")
                .ownerName("Poor John")
                .balance(new BigDecimal("50.00"))
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();

        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(poorAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transactionService.processTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient funds");

        verify(accountRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should correctly debit sender account")
    void testProcessTransaction_DebitSender() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        transactionService.processTransaction(validRequest);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(accountCaptor.capture());

        Account savedFromAccount = accountCaptor.getAllValues().get(0);
        assertThat(savedFromAccount.getBalance()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("Should correctly credit receiver account")
    void testProcessTransaction_CreditReceiver() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        transactionService.processTransaction(validRequest);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(accountCaptor.capture());

        Account savedToAccount = accountCaptor.getAllValues().get(1);
        assertThat(savedToAccount.getBalance()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("Should create debit ledger entry")
    void testProcessTransaction_CreateDebitEntry() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        transactionService.processTransaction(validRequest);

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, times(2)).save(ledgerCaptor.capture());

        LedgerEntry debitEntry = ledgerCaptor.getAllValues().get(0);
        assertThat(debitEntry.getAccountId()).isEqualTo("acc-1");
        assertThat(debitEntry.getAmount()).isEqualByComparingTo("100.00");
        assertThat(debitEntry.getEntryType()).isEqualTo(LedgerEntry.TYPE_DEBIT);
        assertThat(debitEntry.getTransactionId()).isNotNull();
        assertThat(debitEntry.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create credit ledger entry")
    void testProcessTransaction_CreateCreditEntry() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        transactionService.processTransaction(validRequest);

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, times(2)).save(ledgerCaptor.capture());

        LedgerEntry creditEntry = ledgerCaptor.getAllValues().get(1);
        assertThat(creditEntry.getAccountId()).isEqualTo("acc-2");
        assertThat(creditEntry.getAmount()).isEqualByComparingTo("100.00");
        assertThat(creditEntry.getEntryType()).isEqualTo(LedgerEntry.TYPE_CREDIT);
        assertThat(creditEntry.getTransactionId()).isNotNull();
        assertThat(creditEntry.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should emit transaction completed event")
    void testProcessTransaction_EmitEvent() {
        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        transactionService.processTransaction(validRequest);

        ArgumentCaptor<TransactionCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCompletedEvent.class);
        verify(kafkaTemplate).send(eq("transaction-completed"), anyString(), eventCaptor.capture());

        TransactionCompletedEvent event = eventCaptor.getValue();
        assertThat(event.getTransactionId()).isNotNull();
        assertThat(event.getAmount()).isEqualByComparingTo("100.00");
        assertThat(event.getCurrency()).isEqualTo("USD");
        assertThat(event.getFromAccountId()).isEqualTo("acc-1");
        assertThat(event.getToAccountId()).isEqualTo("acc-2");
    }

    @Test
    @DisplayName("Should handle zero amount transaction")
    void testProcessTransaction_ZeroAmount() {
        TransactionRequest zeroRequest = new TransactionRequest(
                "idemp-key-zero",
                "acc-1",
                "acc-2",
                BigDecimal.ZERO,
                "USD"
        );

        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        TransactionResponse response = transactionService.processTransaction(zeroRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should handle large amount transaction")
    void testProcessTransaction_LargeAmount() {
        TransactionRequest largeRequest = new TransactionRequest(
                "idemp-key-large",
                "acc-1",
                "acc-2",
                new BigDecimal("999999.99"),
                "USD"
        );

        Account richAccount = Account.builder()
                .accountId("acc-1")
                .ownerName("Rich John")
                .balance(new BigDecimal("1000000.00"))
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();

        when(idempotencyService.checkAndMark(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(richAccount));
        when(accountRepository.findById("acc-2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        TransactionResponse response = transactionService.processTransaction(largeRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }
}