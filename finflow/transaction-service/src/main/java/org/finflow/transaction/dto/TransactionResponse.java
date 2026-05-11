package org.finflow.transaction.dto;

import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value
public class TransactionResponse {
    UUID transactionId;
    String status;
    Instant timestamp;
}
