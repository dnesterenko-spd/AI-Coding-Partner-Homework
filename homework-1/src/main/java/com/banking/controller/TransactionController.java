package com.banking.controller;

import com.banking.dto.CreateTransactionRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.ValidationErrorResponse;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.service.TransactionService;
import com.banking.validator.TransactionValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for transaction operations.
 * Provides endpoints for creating, retrieving, and filtering transactions.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionValidator validator;

    public TransactionController(TransactionService transactionService, TransactionValidator validator) {
        this.transactionService = transactionService;
        this.validator = validator;
    }

    /**
     * Creates a new transaction.
     *
     * @param request the transaction creation request
     * @return 201 Created with transaction details, or 400 Bad Request if validation fails
     */
    @PostMapping
    public ResponseEntity<Object> createTransaction(@RequestBody CreateTransactionRequest request) {
        // Validate request
        ValidationErrorResponse validationError = validator.validate(request);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
        }

        try {
            TransactionResponse response = transactionService.createTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create transaction: " + e.getMessage());
        }
    }

    /**
     * Retrieves all transactions with optional filters.
     *
     * @param accountId optional filter by account ID
     * @param type optional filter by transaction type (DEPOSIT, WITHDRAWAL, TRANSFER)
     * @param from optional filter by start date (ISO format)
     * @param to optional filter by end date (ISO format)
     * @return 200 OK with list of transactions
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            List<TransactionResponse> transactions = transactionService.getAllTransactions(accountId, type, from, to);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            throw new BadRequestException("Invalid filter parameters: " + e.getMessage());
        }
    }

    /**
     * Retrieves a specific transaction by ID.
     *
     * @param id the transaction ID
     * @return 200 OK with transaction details, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable String id) {
        TransactionResponse transaction = transactionService.getTransactionById(id);
        if (transaction == null) {
            throw new ResourceNotFoundException("Transaction not found with id: " + id);
        }
        return ResponseEntity.ok(transaction);
    }
}
