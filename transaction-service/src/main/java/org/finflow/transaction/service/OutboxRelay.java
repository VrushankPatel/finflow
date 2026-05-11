package org.finflow.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.finflow.transaction.domain.OutboxEvent;
import org.finflow.transaction.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relayEvents() {
        List<OutboxEvent> events = outboxRepository.findByProcessedAtIsNull();
        for (OutboxEvent event : events) {
            try {
                log.info("Relaying outbox event: {}", event.getId());

                // Convert payload back to object or send as string
                // In a production app, we'd use the eventType to determine the class
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());

                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
