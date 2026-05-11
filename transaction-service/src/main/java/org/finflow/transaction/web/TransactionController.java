package org.finflow.transaction.web;

import lombok.RequiredArgsConstructor;
import org.finflow.transaction.dto.TransactionRequest;
import org.finflow.transaction.dto.TransactionResponse;
import org.finflow.transaction.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.processTransaction(request));
    }
}
