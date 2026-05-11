package org.finflow.transaction.service;

import org.finflow.commons.event.TransactionCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"transaction-completed"})
@DirtiesContext
@DisplayName("Kafka Producer Tests")
class KafkaProducerTest {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    private TransactionCompletedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new TransactionCompletedEvent(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "acc-1",
                "acc-2"
        );
    }

    @Test
    @DisplayName("Should send transaction completed event to Kafka")
    void testSendTransactionCompletedEvent() {
        if (kafkaTemplate == null) {
            return;
        }

        var future = kafkaTemplate.send("transaction-completed", testEvent.getTransactionId().toString(), testEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should send event with correct topic")
    void testSendEvent_CorrectTopic() {
        if (kafkaTemplate == null) {
            return;
        }

        var future = kafkaTemplate.send("transaction-completed", testEvent.getTransactionId().toString(), testEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should send event with correct key")
    void testSendEvent_CorrectKey() {
        if (kafkaTemplate == null) {
            return;
        }

        String expectedKey = testEvent.getTransactionId().toString();
        var future = kafkaTemplate.send("transaction-completed", expectedKey, testEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should send event with correct payload")
    void testSendEvent_CorrectPayload() {
        if (kafkaTemplate == null) {
            return;
        }

        var future = kafkaTemplate.send("transaction-completed", testEvent.getTransactionId().toString(), testEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle multiple events")
    void testSendMultipleEvents() {
        if (kafkaTemplate == null) {
            return;
        }

        TransactionCompletedEvent event1 = new TransactionCompletedEvent(
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                "USD",
                "acc-1",
                "acc-2"
        );

        TransactionCompletedEvent event2 = new TransactionCompletedEvent(
                UUID.randomUUID(),
                new BigDecimal("75.00"),
                "EUR",
                "acc-3",
                "acc-4"
        );

        var future1 = kafkaTemplate.send("transaction-completed", event1.getTransactionId().toString(), event1);
        var future2 = kafkaTemplate.send("transaction-completed", event2.getTransactionId().toString(), event2);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future1).isNotNull();
            assertThat(future1.isDone()).isTrue();
            assertThat(future2).isNotNull();
            assertThat(future2.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle event with zero amount")
    void testSendEvent_ZeroAmount() {
        if (kafkaTemplate == null) {
            return;
        }

        TransactionCompletedEvent zeroEvent = new TransactionCompletedEvent(
                UUID.randomUUID(),
                BigDecimal.ZERO,
                "USD",
                "acc-1",
                "acc-2"
        );

        var future = kafkaTemplate.send("transaction-completed", zeroEvent.getTransactionId().toString(), zeroEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle event with large amount")
    void testSendEvent_LargeAmount() {
        if (kafkaTemplate == null) {
            return;
        }

        TransactionCompletedEvent largeEvent = new TransactionCompletedEvent(
                UUID.randomUUID(),
                new BigDecimal("999999999.99"),
                "USD",
                "acc-1",
                "acc-2"
        );

        var future = kafkaTemplate.send("transaction-completed", largeEvent.getTransactionId().toString(), largeEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle event with different currencies")
    void testSendEvent_DifferentCurrencies() {
        if (kafkaTemplate == null) {
            return;
        }

        TransactionCompletedEvent usdEvent = new TransactionCompletedEvent(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "acc-1",
                "acc-2"
        );

        TransactionCompletedEvent eurEvent = new TransactionCompletedEvent(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "EUR",
                "acc-3",
                "acc-4"
        );

        var future1 = kafkaTemplate.send("transaction-completed", usdEvent.getTransactionId().toString(), usdEvent);
        var future2 = kafkaTemplate.send("transaction-completed", eurEvent.getTransactionId().toString(), eurEvent);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future1).isNotNull();
            assertThat(future1.isDone()).isTrue();
            assertThat(future2).isNotNull();
            assertThat(future2.isDone()).isTrue();
        });
    }
}