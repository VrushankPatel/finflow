package org.finflow.transaction.repository;

import org.finflow.transaction.domain.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AccountRepository Tests")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .accountId("test-acc-1")
                .ownerName("Test User")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();
    }

    @Test
    @DisplayName("Should save account successfully")
    void testSave_Account() {
        Account savedAccount = accountRepository.save(testAccount);

        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getAccountId()).isEqualTo("test-acc-1");
        assertThat(savedAccount.getOwnerName()).isEqualTo("Test User");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(savedAccount.getCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(savedAccount.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find account by ID")
    void testFindById_ExistingAccount() {
        entityManager.persist(testAccount);
        entityManager.flush();

        Optional<Account> foundAccount = accountRepository.findById("test-acc-1");

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getAccountId()).isEqualTo("test-acc-1");
        assertThat(foundAccount.get().getOwnerName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when account not found")
    void testFindById_NonExistingAccount() {
        Optional<Account> foundAccount = accountRepository.findById("non-existent");

        assertThat(foundAccount).isEmpty();
    }

    @Test
    @DisplayName("Should update account balance")
    void testUpdate_AccountBalance() {
        entityManager.persist(testAccount);
        entityManager.flush();

        Account account = accountRepository.findById("test-acc-1").orElseThrow();
        account.setBalance(new BigDecimal("2000.00"));
        accountRepository.save(account);

        Account updatedAccount = accountRepository.findById("test-acc-1").orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("Should delete account")
    void testDelete_Account() {
        entityManager.persist(testAccount);
        entityManager.flush();

        accountRepository.deleteById("test-acc-1");

        Optional<Account> deletedAccount = accountRepository.findById("test-acc-1");
        assertThat(deletedAccount).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple accounts")
    void testFindAll_MultipleAccounts() {
        Account account1 = Account.builder()
                .accountId("acc-1")
                .ownerName("User 1")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();

        Account account2 = Account.builder()
                .accountId("acc-2")
                .ownerName("User 2")
                .balance(new BigDecimal("200.00"))
                .currency(Currency.getInstance("EUR"))
                .version(1L)
                .build();

        entityManager.persist(account1);
        entityManager.persist(account2);
        entityManager.flush();

        assertThat(accountRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle account with zero balance")
    void testSave_AccountWithZeroBalance() {
        Account zeroBalanceAccount = Account.builder()
                .accountId("zero-acc")
                .ownerName("Zero Balance User")
                .balance(BigDecimal.ZERO)
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();

        Account savedAccount = accountRepository.save(zeroBalanceAccount);

        assertThat(savedAccount.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Should handle account with large balance")
    void testSave_AccountWithLargeBalance() {
        Account largeBalanceAccount = Account.builder()
                .accountId("large-acc")
                .ownerName("Rich User")
                .balance(new BigDecimal("999999999.99"))
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();

        Account savedAccount = accountRepository.save(largeBalanceAccount);

        assertThat(savedAccount.getBalance()).isEqualByComparingTo("999999999.99");
    }

    @Test
    @DisplayName("Should handle different currencies")
    void testSave_AccountWithDifferentCurrencies() {
        Account usdAccount = Account.builder()
                .accountId("usd-acc")
                .ownerName("USD User")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.getInstance("USD"))
                .version(1L)
                .build();

        Account eurAccount = Account.builder()
                .accountId("eur-acc")
                .ownerName("EUR User")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.getInstance("EUR"))
                .version(1L)
                .build();

        Account gbpAccount = Account.builder()
                .accountId("gbp-acc")
                .ownerName("GBP User")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.getInstance("GBP"))
                .version(1L)
                .build();

        accountRepository.save(usdAccount);
        accountRepository.save(eurAccount);
        accountRepository.save(gbpAccount);

        assertThat(accountRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Should increment version on update")
    void testUpdate_VersionIncrement() {
        entityManager.persist(testAccount);
        entityManager.flush();

        Account account = accountRepository.findById("test-acc-1").orElseThrow();
        Long originalVersion = account.getVersion();
        account.setBalance(new BigDecimal("1500.00"));
        accountRepository.save(account);

        Account updatedAccount = accountRepository.findById("test-acc-1").orElseThrow();
        assertThat(updatedAccount.getVersion()).isGreaterThan(originalVersion);
    }
}