package org.finflow.statement.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {
    @Id
    private UUID statementId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(columnDefinition = "TEXT")
    private String content; // In production, this would be a reference to S3

    @Column(nullable = false)
    private String s3Url;
}
