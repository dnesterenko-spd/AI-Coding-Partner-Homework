package com.banking.controller;

import com.banking.dto.BalanceResponse;
import com.banking.dto.TransactionSummaryResponse;
import com.banking.exception.ResourceNotFoundException;
import com.banking.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for account-related operations.
 * Provides endpoints for balance and transaction summary queries.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final TransactionService transactionService;

    public AccountController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Retrieves the current balance for an account.
     *
     * @param accountId the account ID
     * @return 200 OK with balance details
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getAccountBalance(@PathVariable String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Account ID is required");
        }

        BalanceResponse balance = transactionService.getAccountBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Retrieves transaction summary statistics for an account.
     *
     * @param accountId the account ID
     * @return 200 OK with summary statistics
     */
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<TransactionSummaryResponse> getAccountSummary(@PathVariable String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Account ID is required");
        }

        TransactionSummaryResponse summary = transactionService.getAccountSummary(accountId);
        return ResponseEntity.ok(summary);
    }
}
