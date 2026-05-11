package org.finflow.transaction.service;

import org.finflow.transaction.domain.IdempotencyKey;
import org.finflow.transaction.repository.IdempotencyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository repository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("Should return empty Optional and save key when key is new")
    void testCheckAndMark_NewKey() {
        String key = "new-key";
        when(repository.findById(key)).thenReturn(Optional.empty());

        Optional<String> result = idempotencyService.checkAndMark(key);

        assertTrue(result.isEmpty());
        verify(repository).save(any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("Should return cached payload when key already exists")
    void testCheckAndMark_ExistingKey() {
        String key = "existing-key";
        String payload = "cached-response";
        IdempotencyKey ik = IdempotencyKey.builder().key(key).responsePayload(payload).build();
        when(repository.findById(key)).thenReturn(Optional.of(ik));

        Optional<String> result = idempotencyService.checkAndMark(key);

        assertTrue(result.isPresent());
        assertEquals(payload, result.get());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update response payload for existing key")
    void testUpdateResponse() {
        String key = "key-to-update";
        String newResponse = "final-response";
        IdempotencyKey ik = IdempotencyKey.builder().key(key).responsePayload("old").build();
        when(repository.findById(key)).thenReturn(Optional.of(ik));

        idempotencyService.updateResponse(key, newResponse);

        assertEquals(newResponse, ik.getResponsePayload());
        verify(repository).save(ik);
    }
}
