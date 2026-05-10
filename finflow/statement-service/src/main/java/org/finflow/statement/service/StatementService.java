package org.finflow.statement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.finflow.statement.domain.Statement;
import org.finflow.statement.repository.StatementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {
    private final StatementRepository statementRepository;

    public Statement generateStatement(String accountId) {
        log.info("Generating statement for account: {}", accountId);

        // Mocking statement content generation
        String content = "Statement for account " + accountId + " as of " + Instant.now();
        String s3Url = "s3://finflow-statements/statements/" + accountId + "/" + UUID.randomUUID() + ".pdf";

        Statement statement = Statement.builder()
                .statementId(UUID.randomUUID())
                .accountId(accountId)
                .generatedAt(Instant.now())
                .content(content)
                .s3Url(s3Url)
                .build();

        return statementRepository.save(statement);
    }
}
