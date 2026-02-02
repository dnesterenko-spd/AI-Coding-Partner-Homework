package com.banking.controller;

import com.banking.dto.BalanceResponse;
import com.banking.dto.TransactionSummaryResponse;
import com.banking.exception.ResourceNotFoundException;
import com.banking.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final TransactionService transactionService;

    public AccountController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getAccountBalance(@PathVariable String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Account ID is required");
        }

        BalanceResponse balance = transactionService.getAccountBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/{accountId}/summary")
    public ResponseEntity<?> getAccountSummary(@PathVariable String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Account ID is required");
        }

        TransactionSummaryResponse summary = transactionService.getAccountSummary(accountId);
        return ResponseEntity.ok(summary);
    }
}
