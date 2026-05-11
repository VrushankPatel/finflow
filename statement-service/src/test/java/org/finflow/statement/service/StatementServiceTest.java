package org.finflow.statement.service;

import org.finflow.statement.domain.Statement;
import org.finflow.statement.repository.StatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatementService Tests")
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepository;

    @InjectMocks
    private StatementService statementService;

    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = "acc-123";
    }

    @Test
    @DisplayName("Should generate statement successfully")
    void testGenerateStatement_Success() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(testAccountId);

        assertThat(statement).isNotNull();
        assertThat(statement.getAccountId()).isEqualTo(testAccountId);
        assertThat(statement.getStatementId()).isNotNull();
        assertThat(statement.getGeneratedAt()).isNotNull();
        assertThat(statement.getContent()).isNotEmpty();
        assertThat(statement.getS3Url()).isNotEmpty();
        assertThat(statement.getS3Url()).startsWith("s3://finflow-statements/statements/");
        assertThat(statement.getS3Url()).endsWith(".pdf");
    }

    @Test
    @DisplayName("Should save statement to repository")
    void testGenerateStatement_SaveToRepository() {
        ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);
        when(statementRepository.save(statementCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        statementService.generateStatement(testAccountId);

        verify(statementRepository, times(1)).save(any(Statement.class));

        Statement savedStatement = statementCaptor.getValue();
        assertThat(savedStatement.getAccountId()).isEqualTo(testAccountId);
        assertThat(savedStatement.getStatementId()).isNotNull();
    }

    @Test
    @DisplayName("Should generate unique statement ID")
    void testGenerateStatement_UniqueId() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement1 = statementService.generateStatement(testAccountId);
        Statement statement2 = statementService.generateStatement(testAccountId);

        assertThat(statement1.getStatementId()).isNotEqualTo(statement2.getStatementId());
    }

    @Test
    @DisplayName("Should include account ID in statement content")
    void testGenerateStatement_ContentIncludesAccountId() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(testAccountId);

        assertThat(statement.getContent()).contains(testAccountId);
    }

    @Test
    @DisplayName("Should include timestamp in statement content")
    void testGenerateStatement_ContentIncludesTimestamp() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeGeneration = Instant.now();
        Statement statement = statementService.generateStatement(testAccountId);
        Instant afterGeneration = Instant.now();

        assertThat(statement.getGeneratedAt()).isBetween(beforeGeneration, afterGeneration);
        assertThat(statement.getContent()).contains("as of");
    }

    @Test
    @DisplayName("Should generate S3 URL with account ID")
    void testGenerateStatement_S3UrlWithAccountId() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(testAccountId);

        assertThat(statement.getS3Url()).contains("/statements/" + testAccountId + "/");
    }

    @Test
    @DisplayName("Should generate S3 URL with unique filename")
    void testGenerateStatement_S3UrlWithUniqueFilename() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement1 = statementService.generateStatement(testAccountId);
        Statement statement2 = statementService.generateStatement(testAccountId);

        assertThat(statement1.getS3Url()).isNotEqualTo(statement2.getS3Url());
    }

    @Test
    @DisplayName("Should handle empty account ID")
    void testGenerateStatement_EmptyAccountId() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement("");

        assertThat(statement).isNotNull();
        assertThat(statement.getAccountId()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null account ID")
    void testGenerateStatement_NullAccountId() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(null);

        assertThat(statement).isNotNull();
        assertThat(statement.getAccountId()).isNull();
    }

    @Test
    @DisplayName("Should handle special characters in account ID")
    void testGenerateStatement_SpecialCharactersInAccountId() {
        String specialAccountId = "acc-123!@#$%^&*()";
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(specialAccountId);

        assertThat(statement).isNotNull();
        assertThat(statement.getAccountId()).isEqualTo(specialAccountId);
        assertThat(statement.getContent()).contains(specialAccountId);
    }

    @Test
    @DisplayName("Should handle long account ID")
    void testGenerateStatement_LongAccountId() {
        String longAccountId = "acc-" + "x".repeat(1000);
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(longAccountId);

        assertThat(statement).isNotNull();
        assertThat(statement.getAccountId()).isEqualTo(longAccountId);
    }

    @Test
    @DisplayName("Should generate statement with PDF extension")
    void testGenerateStatement_PdfExtension() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(testAccountId);

        assertThat(statement.getS3Url()).endsWith(".pdf");
    }

    @Test
    @DisplayName("Should generate statement with correct S3 bucket path")
    void testGenerateStatement_S3BucketPath() {
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement statement = statementService.generateStatement(testAccountId);

        assertThat(statement.getS3Url()).startsWith("s3://finflow-statements/");
    }
}