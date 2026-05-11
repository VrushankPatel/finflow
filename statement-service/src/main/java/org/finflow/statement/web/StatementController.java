package org.finflow.statement.web;

import lombok.RequiredArgsConstructor;
import org.finflow.statement.domain.Statement;
import org.finflow.statement.service.StatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {
    private final StatementService statementService;

    @GetMapping("/{accountId}")
    public ResponseEntity<Statement> getStatement(@PathVariable String accountId) {
        return ResponseEntity.ok(statementService.generateStatement(accountId));
    }
}