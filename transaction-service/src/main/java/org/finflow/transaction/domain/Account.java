package org.finflow.transaction.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Currency;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    private String accountId;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private Currency currency;

    @Version
    private Long version; // Optimistic locking for concurrency control

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account: " + accountId);
        }
        this.balance = this.balance.subtract(amount);
    }
}

class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
