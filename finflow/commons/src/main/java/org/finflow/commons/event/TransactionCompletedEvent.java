package org.finflow.commons.event;

import lombok.Value;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
public class TransactionCompletedEvent extends BaseEvent {
    UUID transactionId;
    BigDecimal amount;
    String currency;
    String fromAccountId;
    String toAccountId;
}
