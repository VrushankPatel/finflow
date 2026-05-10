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
            log.info("Duplicate request detected for key: {}", key);
            return Optional.ofNullable(existing.get().getResponsePayload());
        }

        repository.save(IdempotencyKey.builder()
                .key(key)
                .createdAt(Instant.now())
                .build());
        return Optional.empty();
    }

    @Transactional
    public void updateResponse(String key, String response) {
        repository.findById(key).ifPresent(ik -> {
            ik.setResponsePayload(response);
            repository.save(ik);
        });
    }
}
