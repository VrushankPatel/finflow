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

    @Column(columnDefinition = "TEXT")
    private String responsePayload;
}
