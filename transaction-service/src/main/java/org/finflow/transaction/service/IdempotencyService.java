package org.finflow.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.finflow.transaction.domain.IdempotencyKey;
import org.finflow.transaction.repository.IdempotencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepository repository;

    @Transactional
    public Optional<String> checkAndMark(String key) {
        Optional<IdempotencyKey> existing = repository.findById(key);
        if (existing.isPresent()) {
            IdempotencyKey ik = existing.get();
            if (IdempotencyKey.STATUS_COMPLETED.equals(ik.getStatus())) {
                log.info("Duplicate request detected (COMPLETED) for key: {}", key);
                return Optional.ofNullable(ik.getResponsePayload());
            }
            if (IdempotencyKey.STATUS_STARTED.equals(ik.getStatus())) {
                // Check if the transaction is stuck (e.g., > 30 seconds)
                if (ik.getCreatedAt().plusSeconds(30).isBefore(Instant.now())) {
                    log.warn("Detected stuck transaction for key: {}. Allowing retry.", key);
                    ik.setStatus(IdempotencyKey.STATUS_STARTED);
                    ik.setCreatedAt(Instant.now());
                    repository.save(ik);
                    return Optional.empty();
                }
                log.info("Request already in progress for key: {}", key);
                return Optional.of("Request already in progress");
            }
        }

        repository.save(IdempotencyKey.builder()
                .key(key)
                .createdAt(Instant.now())
                .status(IdempotencyKey.STATUS_STARTED)
                .build());
        return Optional.empty();
    }

    @Transactional
    public void updateResponse(String key, String response, boolean success) {
        repository.findById(key).ifPresent(ik -> {
            ik.setResponsePayload(response);
            ik.setStatus(success ? IdempotencyKey.STATUS_COMPLETED : IdempotencyKey.STATUS_FAILED);
            repository.save(ik);
        });
    }
}
