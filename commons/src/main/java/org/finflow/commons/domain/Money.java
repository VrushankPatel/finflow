package org.finflow.commons.domain;

import lombok.Value;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing money in a specific currency.
 * Ensures precision for financial calculations using BigDecimal.
 */
@Value
public class Money {
    BigDecimal amount;
    Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currency = currency;
    }

    public Money add(Money other) {
        validateCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    private void validateCurrency(Money other) {
        if (!Objects.equals(this.currency, other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }
}
