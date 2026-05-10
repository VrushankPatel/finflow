package org.finflow.statement.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.finflow.commons.event.TransactionCompletedEvent;
import org.finflow.statement.service.StatementService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
    private final StatementService statementService;

    @KafkaListener(topics = "transaction-completed", groupId = "statement-group")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Received transaction completion event: {}", event.getTransactionId());

        // Trigger statement update for both accounts involved in the transaction
        statementService.generateStatement(event.getFromAccountId());
        statementService.generateStatement(event.getToAccountId());
    }
}
