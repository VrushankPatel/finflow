package org.finflow.transaction.repository;

import org.finflow.transaction.domain.LedgerEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LedgerRepository Tests")
class LedgerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LedgerRepository ledgerRepository;

    private UUID transactionId;
    private LedgerEntry debitEntry;
    private LedgerEntry creditEntry;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        Currency usd = Currency.getInstance("USD");

        debitEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId("acc-1")
                .amount(new BigDecimal("100.00"))
                .entryType(LedgerEntry.TYPE_DEBIT)
                .currency(usd)
                .createdAt(Instant.now())
                .build();

        creditEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId("acc-2")
                .amount(new BigDecimal("100.00"))
                .entryType(LedgerEntry.TYPE_CREDIT)
                .currency(usd)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save ledger entry successfully")
    void testSave_LedgerEntry() {
        LedgerEntry savedEntry = ledgerRepository.save(debitEntry);

        assertThat(savedEntry).isNotNull();
        assertThat(savedEntry.getId()).isNotNull();
        assertThat(savedEntry.getTransactionId()).isEqualTo(transactionId);
        assertThat(savedEntry.getAccountId()).isEqualTo("acc-1");
        assertThat(savedEntry.getAmount()).isEqualByComparingTo("100.00");
        assertThat(savedEntry.getEntryType()).isEqualTo(LedgerEntry.TYPE_DEBIT);
        assertThat(savedEntry.getCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(savedEntry.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find ledger entry by ID")
    void testFindById_ExistingEntry() {
        LedgerEntry savedEntry = ledgerRepository.save(debitEntry);

        LedgerEntry foundEntry = ledgerRepository.findById(savedEntry.getId()).orElse(null);

        assertThat(foundEntry).isNotNull();
        assertThat(foundEntry.getId()).isEqualTo(savedEntry.getId());
        assertThat(foundEntry.getTransactionId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("Should return empty when entry not found")
    void testFindById_NonExistingEntry() {
        Optional<LedgerEntry> foundEntry = ledgerRepository.findById(999L);

        assertThat(foundEntry).isEmpty();
    }

    @Test
    @DisplayName("Should find all ledger entries")
    void testFindAll_AllEntries() {
        ledgerRepository.save(debitEntry);
        ledgerRepository.save(creditEntry);

        List<LedgerEntry> allEntries = ledgerRepository.findAll();

        assertThat(allEntries).hasSize(2);
    }

    @Test
    @DisplayName("Should find entries by transaction ID")
    void testFindByTransactionId() {
        ledgerRepository.save(debitEntry);
        ledgerRepository.save(creditEntry);

        List<LedgerEntry> entries = ledgerRepository.findAll().stream()
                .filter(e -> e.getTransactionId().equals(transactionId))
                .toList();

        assertThat(entries).hasSize(2);
    }

    @Test
    @DisplayName("Should find entries by account ID")
    void testFindByAccountId() {
        ledgerRepository.save(debitEntry);
        ledgerRepository.save(creditEntry);

        List<LedgerEntry> entries = ledgerRepository.findAll().stream()
                .filter(e -> e.getAccountId().equals("acc-1"))
                .toList();

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.TYPE_DEBIT);
    }

    @Test
    @DisplayName("Should handle debit entry type")
    void testSave_DebitEntry() {
        LedgerEntry savedEntry = ledgerRepository.save(debitEntry);

        assertThat(savedEntry.getEntryType()).isEqualTo(LedgerEntry.TYPE_DEBIT);
    }

    @Test
    @DisplayName("Should handle credit entry type")
    void testSave_CreditEntry() {
        LedgerEntry savedEntry = ledgerRepository.save(creditEntry);

        assertThat(savedEntry.getEntryType()).isEqualTo(LedgerEntry.TYPE_CREDIT);
    }

    @Test
    @DisplayName("Should handle different currencies")
    void testSave_DifferentCurrencies() {
        LedgerEntry usdEntry = LedgerEntry.builder()
                .transactionId(UUID.randomUUID())
                .accountId("acc-1")
                .amount(new BigDecimal("100.00"))
                .entryType(LedgerEntry.TYPE_DEBIT)
                .currency(Currency.getInstance("USD"))
                .createdAt(Instant.now())
                .build();

        LedgerEntry eurEntry = LedgerEntry.builder()
                .transactionId(UUID.randomUUID())
                .accountId("acc-2")
                .amount(new BigDecimal("100.00"))
                .entryType(LedgerEntry.TYPE_CREDIT)
                .currency(Currency.getInstance("EUR"))
                .createdAt(Instant.now())
                .build();

        ledgerRepository.save(usdEntry);
        ledgerRepository.save(eurEntry);

        assertThat(ledgerRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle zero amount")
    void testSave_ZeroAmount() {
        LedgerEntry zeroEntry = LedgerEntry.builder()
                .transactionId(UUID.randomUUID())
                .accountId("acc-1")
                .amount(BigDecimal.ZERO)
                .entryType(LedgerEntry.TYPE_DEBIT)
                .currency(Currency.getInstance("USD"))
                .createdAt(Instant.now())
                .build();

        LedgerEntry savedEntry = ledgerRepository.save(zeroEntry);

        assertThat(savedEntry.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Should handle large amount")
    void testSave_LargeAmount() {
        LedgerEntry largeEntry = LedgerEntry.builder()
                .transactionId(UUID.randomUUID())
                .accountId("acc-1")
                .amount(new BigDecimal("999999999.99"))
                .entryType(LedgerEntry.TYPE_DEBIT)
                .currency(Currency.getInstance("USD"))
                .createdAt(Instant.now())
                .build();

        LedgerEntry savedEntry = ledgerRepository.save(largeEntry);

        assertThat(savedEntry.getAmount()).isEqualByComparingTo("999999999.99");
    }

    @Test
    @DisplayName("Should delete ledger entry")
    void testDelete_LedgerEntry() {
        LedgerEntry savedEntry = ledgerRepository.save(debitEntry);

        ledgerRepository.deleteById(savedEntry.getId());

        Optional<LedgerEntry> deletedEntry = ledgerRepository.findById(savedEntry.getId());
        assertThat(deletedEntry).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple transactions")
    void testSave_MultipleTransactions() {
        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();

        LedgerEntry entry1 = LedgerEntry.builder()
                .transactionId(txId1)
                .accountId("acc-1")
                .amount(new BigDecimal("100.00"))
                .entryType(LedgerEntry.TYPE_DEBIT)
                .currency(Currency.getInstance("USD"))
                .createdAt(Instant.now())
                .build();

        LedgerEntry entry2 = LedgerEntry.builder()
                .transactionId(txId2)
                .accountId("acc-2")
                .amount(new BigDecimal("200.00"))
                .entryType(LedgerEntry.TYPE_CREDIT)
                .currency(Currency.getInstance("USD"))
                .createdAt(Instant.now())
                .build();

        ledgerRepository.save(entry1);
        ledgerRepository.save(entry2);

        assertThat(ledgerRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should preserve creation timestamp")
    void testSave_CreationTimestamp() {
        Instant beforeSave = Instant.now();
        LedgerEntry savedEntry = ledgerRepository.save(debitEntry);
        Instant afterSave = Instant.now();

        assertThat(savedEntry.getCreatedAt()).isBetween(beforeSave, afterSave);
    }
}