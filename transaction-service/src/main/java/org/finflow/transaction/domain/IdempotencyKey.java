package org.finflow.transaction.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {
    @Id
    private String key;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String status; // STARTED, COMPLETED, FAILED

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    public static final String STATUS_STARTED = "STARTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}
