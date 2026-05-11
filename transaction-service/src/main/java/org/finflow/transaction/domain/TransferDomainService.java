package org.finflow.transaction.domain;

import lombok.RequiredArgsConstructor;
import org.finflow.transaction.repository.AccountRepository;
import org.finflow.transaction.repository.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferDomainService {
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional
    public void transferFunds(UUID txId, String fromAccountId, String toAccountId, BigDecimal amount) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        // Debit sender
        fromAccount.debit(amount);
        accountRepository.save(fromAccount);

        // Credit receiver
        toAccount.credit(amount);
        accountRepository.save(toAccount);

        // Record Ledger Entries
        createLedgerEntry(txId, fromAccount.getAccountId(), amount, LedgerEntry.TYPE_DEBIT, fromAccount.getCurrency());
        createLedgerEntry(txId, toAccount.getAccountId(), amount, LedgerEntry.TYPE_CREDIT, toAccount.getCurrency());
    }

    private void createLedgerEntry(UUID txId, String accountId, BigDecimal amount, String type, java.util.Currency currency) {
        ledgerRepository.save(LedgerEntry.builder()
                .transactionId(txId)
                .accountId(accountId)
                .amount(amount)
                .entryType(type)
                .currency(currency)
                .createdAt(Instant.now())
                .build());
    }
}
