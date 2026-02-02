package com.banking.service;

import com.banking.dto.CreateTransactionRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransactionSummaryResponse;
import com.banking.dto.BalanceResponse;
import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import com.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                request.getCurrency().toUpperCase(),
                TransactionType.valueOf(request.getType().toUpperCase()),
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
        );

        Transaction saved = repository.save(transaction);
        return mapToResponse(saved);
    }

    public TransactionResponse getTransactionById(String id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .orElse(null);
    }

    public List<TransactionResponse> getAllTransactions(
            String accountId,
            String type,
            String from,
            String to) {

        List<Transaction> transactions = repository.findAll();

        // Filter by account ID
        if (accountId != null && !accountId.isBlank()) {
            transactions = transactions.stream()
                    .filter(t -> (t.getFromAccount() != null && t.getFromAccount().equals(accountId)) ||
                               (t.getToAccount() != null && t.getToAccount().equals(accountId)))
                    .collect(Collectors.toList());
        }

        // Filter by type
        if (type != null && !type.isBlank()) {
            try {
                TransactionType txType = TransactionType.valueOf(type.toUpperCase());
                transactions = transactions.stream()
                        .filter(t -> t.getType() == txType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid type, return empty list
                return Collections.emptyList();
            }
        }

        // Filter by date range
        if ((from != null && !from.isBlank()) || (to != null && !to.isBlank())) {
            LocalDateTime fromDate = from != null && !from.isBlank() ?
                    LocalDateTime.parse(from) : LocalDateTime.MIN;
            LocalDateTime toDate = to != null && !to.isBlank() ?
                    LocalDateTime.parse(to) : LocalDateTime.MAX;

            transactions = transactions.stream()
                    .filter(t -> !t.getTimestamp().isBefore(fromDate) &&
                               !t.getTimestamp().isAfter(toDate))
                    .collect(Collectors.toList());
        }

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BalanceResponse getAccountBalance(String accountId) {
        List<Transaction> accountTransactions = repository.findByAccountId(accountId);

        BigDecimal balance = BigDecimal.ZERO;
        String currency = "USD"; // Default currency

        for (Transaction tx : accountTransactions) {
            if (tx.getStatus() == TransactionStatus.COMPLETED) {
                currency = tx.getCurrency();

                if (tx.getType() == TransactionType.DEPOSIT ||
                    (tx.getType() == TransactionType.TRANSFER && accountId.equals(tx.getToAccount()))) {
                    balance = balance.add(tx.getAmount());
                } else if (tx.getType() == TransactionType.WITHDRAWAL ||
                          (tx.getType() == TransactionType.TRANSFER && accountId.equals(tx.getFromAccount()))) {
                    balance = balance.subtract(tx.getAmount());
                }
            }
        }

        return new BalanceResponse(accountId, balance, currency);
    }

    public TransactionSummaryResponse getAccountSummary(String accountId) {
        List<Transaction> accountTransactions = repository.findByAccountId(accountId);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        LocalDateTime mostRecentDate = null;

        for (Transaction tx : accountTransactions) {
            if (tx.getStatus() == TransactionStatus.COMPLETED) {
                if (tx.getType() == TransactionType.DEPOSIT ||
                    (tx.getType() == TransactionType.TRANSFER && accountId.equals(tx.getToAccount()))) {
                    totalDeposits = totalDeposits.add(tx.getAmount());
                } else if (tx.getType() == TransactionType.WITHDRAWAL ||
                          (tx.getType() == TransactionType.TRANSFER && accountId.equals(tx.getFromAccount()))) {
                    totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                }

                if (mostRecentDate == null || tx.getTimestamp().isAfter(mostRecentDate)) {
                    mostRecentDate = tx.getTimestamp();
                }
            }
        }

        return new TransactionSummaryResponse(
                accountId,
                totalDeposits,
                totalWithdrawals,
                accountTransactions.size(),
                mostRecentDate
        );
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType().name(),
                transaction.getTimestamp(),
                transaction.getStatus().name()
        );
    }
}
