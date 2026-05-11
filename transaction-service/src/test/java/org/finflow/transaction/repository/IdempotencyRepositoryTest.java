package org.finflow.transaction.repository;

import org.finflow.transaction.domain.IdempotencyKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("IdempotencyRepository Tests")
class IdempotencyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    private IdempotencyKey testKey;

    @BeforeEach
    void setUp() {
        testKey = IdempotencyKey.builder()
                .key("test-key-123")
                .createdAt(Instant.now())
                .responsePayload("{\"transactionId\":\"tx-123\",\"status\":\"COMPLETED\"}")
                .build();
    }

    @Test
    @DisplayName("Should save idempotency key successfully")
    void testSave_IdempotencyKey() {
        IdempotencyKey savedKey = idempotencyRepository.save(testKey);

        assertThat(savedKey).isNotNull();
        assertThat(savedKey.getKey()).isEqualTo("test-key-123");
        assertThat(savedKey.getCreatedAt()).isNotNull();
        assertThat(savedKey.getResponsePayload()).isEqualTo("{\"transactionId\":\"tx-123\",\"status\":\"COMPLETED\"}");
    }

    @Test
    @DisplayName("Should find idempotency key by ID")
    void testFindById_ExistingKey() {
        entityManager.persist(testKey);
        entityManager.flush();

        Optional<IdempotencyKey> foundKey = idempotencyRepository.findById("test-key-123");

        assertThat(foundKey).isPresent();
        assertThat(foundKey.get().getKey()).isEqualTo("test-key-123");
        assertThat(foundKey.get().getResponsePayload()).isEqualTo("{\"transactionId\":\"tx-123\",\"status\":\"COMPLETED\"}");
    }

    @Test
    @DisplayName("Should return empty when key not found")
    void testFindById_NonExistingKey() {
        Optional<IdempotencyKey> foundKey = idempotencyRepository.findById("non-existent-key");

        assertThat(foundKey).isEmpty();
    }

    @Test
    @DisplayName("Should update response payload")
    void testUpdate_ResponsePayload() {
        entityManager.persist(testKey);
        entityManager.flush();

        IdempotencyKey key = idempotencyRepository.findById("test-key-123").orElseThrow();
        key.setResponsePayload("{\"transactionId\":\"tx-456\",\"status\":\"FAILED\"}");
        idempotencyRepository.save(key);

        IdempotencyKey updatedKey = idempotencyRepository.findById("test-key-123").orElseThrow();
        assertThat(updatedKey.getResponsePayload()).isEqualTo("{\"transactionId\":\"tx-456\",\"status\":\"FAILED\"}");
    }

    @Test
    @DisplayName("Should delete idempotency key")
    void testDelete_IdempotencyKey() {
        entityManager.persist(testKey);
        entityManager.flush();

        idempotencyRepository.deleteById("test-key-123");

        Optional<IdempotencyKey> deletedKey = idempotencyRepository.findById("test-key-123");
        assertThat(deletedKey).isEmpty();
    }

    @Test
    @DisplayName("Should handle key without response payload")
    void testSave_KeyWithoutResponsePayload() {
        IdempotencyKey keyWithoutPayload = IdempotencyKey.builder()
                .key("key-no-payload")
                .createdAt(Instant.now())
                .build();

        IdempotencyKey savedKey = idempotencyRepository.save(keyWithoutPayload);

        assertThat(savedKey.getResponsePayload()).isNull();
    }

    @Test
    @DisplayName("Should handle long response payload")
    void testSave_LongResponsePayload() {
        String longPayload = "{\"transactionId\":\"tx-123\",\"status\":\"COMPLETED\",\"data\":\"" + "x".repeat(10000) + "\"}";

        IdempotencyKey keyWithLongPayload = IdempotencyKey.builder()
                .key("key-long-payload")
                .createdAt(Instant.now())
                .responsePayload(longPayload)
                .build();

        IdempotencyKey savedKey = idempotencyRepository.save(keyWithLongPayload);

        assertThat(savedKey.getResponsePayload()).hasSize(longPayload.length());
    }

    @Test
    @DisplayName("Should handle multiple keys")
    void testFindAll_MultipleKeys() {
        IdempotencyKey key1 = IdempotencyKey.builder()
                .key("key-1")
                .createdAt(Instant.now())
                .responsePayload("{\"status\":\"COMPLETED\"}")
                .build();

        IdempotencyKey key2 = IdempotencyKey.builder()
                .key("key-2")
                .createdAt(Instant.now())
                .responsePayload("{\"status\":\"FAILED\"}")
                .build();

        idempotencyRepository.save(key1);
        idempotencyRepository.save(key2);

        assertThat(idempotencyRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should preserve creation timestamp")
    void testSave_CreationTimestamp() {
        Instant beforeSave = Instant.now();
        IdempotencyKey savedKey = idempotencyRepository.save(testKey);
        Instant afterSave = Instant.now();

        assertThat(savedKey.getCreatedAt()).isBetween(beforeSave, afterSave);
    }

    @Test
    @DisplayName("Should handle special characters in key")
    void testSave_SpecialCharactersInKey() {
        IdempotencyKey keyWithSpecialChars = IdempotencyKey.builder()
                .key("key-with-special-chars-!@#$%^&*()")
                .createdAt(Instant.now())
                .responsePayload("{\"status\":\"OK\"}")
                .build();

        IdempotencyKey savedKey = idempotencyRepository.save(keyWithSpecialChars);

        assertThat(savedKey.getKey()).isEqualTo("key-with-special-chars-!@#$%^&*()");
    }

    @Test
    @DisplayName("Should handle JSON response payload")
    void testSave_JsonResponsePayload() {
        String jsonPayload = """
                {
                    "transactionId": "tx-789",
                    "status": "COMPLETED",
                    "timestamp": "2024-01-01T00:00:00Z",
                    "amount": 100.00,
                    "currency": "USD"
                }
                """;

        IdempotencyKey keyWithJson = IdempotencyKey.builder()
                .key("key-json")
                .createdAt(Instant.now())
                .responsePayload(jsonPayload)
                .build();

        IdempotencyKey savedKey = idempotencyRepository.save(keyWithJson);

        assertThat(savedKey.getResponsePayload()).isEqualTo(jsonPayload);
    }

    @Test
    @DisplayName("Should handle empty response payload")
    void testSave_EmptyResponsePayload() {
        IdempotencyKey keyWithEmptyPayload = IdempotencyKey.builder()
                .key("key-empty-payload")
                .createdAt(Instant.now())
                .responsePayload("")
                .build();

        IdempotencyKey savedKey = idempotencyRepository.save(keyWithEmptyPayload);

        assertThat(savedKey.getResponsePayload()).isEmpty();
    }
}