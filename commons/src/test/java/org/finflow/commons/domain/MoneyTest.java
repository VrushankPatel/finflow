package org.finflow.commons.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("Should correctly add two amounts of the same currency")
    void testAdd() {
        Currency usd = Currency.getInstance("USD");
        Money m1 = new Money(new BigDecimal("100.00"), usd);
        Money m2 = new Money(new BigDecimal("50.00"), usd);

        Money result = m1.add(m2);

        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals(usd, result.getCurrency());
    }

    @Test
    @DisplayName("Should correctly subtract two amounts of the same currency")
    void testSubtract() {
        Currency usd = Currency.getInstance("USD");
        Money m1 = new Money(new BigDecimal("100.00"), usd);
        Money m2 = new Money(new BigDecimal("50.00"), usd);

        Money result = m1.subtract(m2);

        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals(usd, result.getCurrency());
    }

    @Test
    @DisplayName("Should throw exception when adding different currencies")
    void testCurrencyMismatch() {
        Currency usd = Currency.getInstance("USD");
        Currency eur = Currency.getInstance("EUR");
        Money m1 = new Money(new BigDecimal("100.00"), usd);
        Money m2 = new Money(new BigDecimal("100.00"), eur);

        assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
    }

    @Test
    @DisplayName("Should handle rounding to 2 decimal places")
    void testRounding() {
        Currency usd = Currency.getInstance("USD");
        Money m1 = new Money(new BigDecimal("10.123"), usd);
        Money m2 = new Money(new BigDecimal("10.126"), usd);

        assertEquals(new BigDecimal("10.12"), m1.getAmount());
        assertEquals(new BigDecimal("10.13"), m2.getAmount());
    }
}
