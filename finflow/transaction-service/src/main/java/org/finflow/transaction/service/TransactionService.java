package org.finflow.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.finflow.commons.event.TransactionCompletedEvent;
import org.finflow.transaction.domain.Account;
import org.finflow.transaction.domain.LedgerEntry;
import org.finflow.transaction.dto.TransactionRequest;
import org.finflow.transaction.dto.TransactionResponse;
import org.finflow.transaction.repository.AccountRepository;
import org.finflow.transaction.repository.LedgerRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyService idempotencyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        // 1. Idempotency Check
        var cachedResponse = idempotencyService.checkAndMark(request.getIdempotencyKey());
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", request.getIdempotencyKey());
            // In a real app, we'd deserialize the responsePayload JSON. Simplified here.
            return null;
        }

        UUID txId = UUID.randomUUID();
        log.info("Processing transaction {}: {} -> {} amount {}", txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // 2. Double-Entry Ledger Logic
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        // Debit sender
        fromAccount.debit(request.getAmount());
        accountRepository.save(fromAccount);

        // Credit receiver
        toAccount.credit(request.getAmount());
        accountRepository.save(toAccount);

        // Record Ledger Entries (The Audit Trail)
        createLedgerEntry(txId, fromAccount.getAccountId(), request.getAmount(), LedgerEntry.TYPE_DEBIT, fromAccount.getCurrency());
        createLedgerEntry(txId, toAccount.getAccountId(), request.getAmount(), LedgerEntry.TYPE_CREDIT, toAccount.getCurrency());

        // 3. Emit Event to Kafka
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                txId,
                request.getAmount(),
                request.getCurrency(),
                request.getFromAccountId(),
                request.getToAccountId()
        );
        kafkaTemplate.send("transaction-completed", txId.toString(), event);

        TransactionResponse response = new TransactionResponse(txId, "COMPLETED", Instant.now());

        // Update idempotency key with the result
        idempotencyService.updateResponse(request.getIdempotencyKey(), response.toString());

        return response;
    }

    private void createLedgerEntry(UUID txId, String accountId, java.math.BigDecimal amount, String type, java.util.Currency currency) {
        ledgerRepository.save(LedgerEntry.builder()
                .transactionId(txId)
                .accountId(accountId)
                .amount(amount)
                .entryType(type)
                .currency(currency)
                .createdAt(Instant.now())
                .build());
    }
}
