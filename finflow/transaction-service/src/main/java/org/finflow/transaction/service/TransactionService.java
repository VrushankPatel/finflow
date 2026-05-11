package org.finflow.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.finflow.commons.event.TransactionCompletedEvent;
import org.finflow.transaction.domain.Account;
import org.finflow.transaction.domain.LedgerEntry;
import org.finflow.transaction.domain.OutboxEvent;
import org.finflow.transaction.domain.TransferDomainService;
import org.finflow.transaction.dto.TransactionRequest;
import org.finflow.transaction.dto.TransactionResponse;
import org.finflow.transaction.repository.AccountRepository;
import org.finflow.transaction.repository.LedgerRepository;
import org.finflow.transaction.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransferDomainService transferDomainService;
    private final IdempotencyService idempotencyService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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

        // 2. Execute Transfer via Domain Service
        transferDomainService.transferFunds(txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // 3. Record Event in Outbox
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                txId,
                request.getAmount(),
                request.getCurrency(),
                request.getFromAccountId(),
                request.getToAccountId()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("TRANSACTION")
                    .aggregateId(txId.toString())
                    .eventType("transaction-completed")
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to serialize transaction event: {}", e.getMessage());
            throw new RuntimeException("Event serialization failed", e);
        }

        TransactionResponse response = new TransactionResponse(txId, "COMPLETED", Instant.now());

        // Update idempotency key with the result
        idempotencyService.updateResponse(request.getIdempotencyKey(), response.toString(), true);

        return response;
    }

    }
}
