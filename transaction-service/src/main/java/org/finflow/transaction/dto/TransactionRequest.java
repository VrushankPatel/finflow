package org.finflow.transaction.dto;

import lombok.Value;
import java.math.BigDecimal;

@Value
public class TransactionRequest {
    String idempotencyKey;
    String fromAccountId;
    String toAccountId;
    BigDecimal amount;
    String currency;
}
