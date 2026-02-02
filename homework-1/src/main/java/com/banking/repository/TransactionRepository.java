package com.banking.repository;

import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Repository for transaction data access.
 * Uses in-memory storage with CopyOnWriteArrayList for thread safety.
 * Initializes with sample seed data on startup.
 */
@Repository
public class TransactionRepository {
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

    public TransactionRepository() {
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Sample seed data
        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                null,
                "ACC-12345",
                new BigDecimal("500.00"),
                "USD",
                TransactionType.DEPOSIT,
                LocalDateTime.now().minusDays(5),
                TransactionStatus.COMPLETED
        ));

        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                "ACC-12345",
                "ACC-67890",
                new BigDecimal("150.50"),
                "USD",
                TransactionType.TRANSFER,
                LocalDateTime.now().minusDays(3),
                TransactionStatus.COMPLETED
        ));

        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                "ACC-12345",
                null,
                new BigDecimal("100.00"),
                "USD",
                TransactionType.WITHDRAWAL,
                LocalDateTime.now().minusDays(2),
                TransactionStatus.COMPLETED
        ));

        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                null,
                "ACC-67890",
                new BigDecimal("250.00"),
                "EUR",
                TransactionType.DEPOSIT,
                LocalDateTime.now().minusDays(4),
                TransactionStatus.COMPLETED
        ));

        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                "ACC-67890",
                "ACC-11111",
                new BigDecimal("75.25"),
                "EUR",
                TransactionType.TRANSFER,
                LocalDateTime.now().minusDays(1),
                TransactionStatus.COMPLETED
        ));

        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                null,
                "ACC-11111",
                new BigDecimal("1000.00"),
                "GBP",
                TransactionType.DEPOSIT,
                LocalDateTime.now().minusDays(10),
                TransactionStatus.COMPLETED
        ));

        transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                "ACC-11111",
                "ACC-12345",
                new BigDecimal("200.00"),
                "GBP",
                TransactionType.TRANSFER,
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
        ));
    }

    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    public Optional<Transaction> findById(String id) {
        return transactions.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    public List<Transaction> findAll() {
        return new ArrayList<>(transactions);
    }

    /**
     * Finds all transactions associated with an account (as either source or destination).
     *
     * @param accountId the account ID to search for
     * @return list of matching transactions
     */
    public List<Transaction> findByAccountId(String accountId) {
        return transactions.stream()
                .filter(t -> (t.getFromAccount() != null && t.getFromAccount().equals(accountId)) ||
                           (t.getToAccount() != null && t.getToAccount().equals(accountId)))
                .collect(Collectors.toList());
    }

    /**
     * Finds all transactions of a specific type.
     *
     * @param type the transaction type to filter by
     * @return list of matching transactions
     */
    public List<Transaction> findByType(TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Finds all transactions within a date range (inclusive).
     *
     * @param from start date/time
     * @param to end date/time
     * @return list of matching transactions
     */
    public List<Transaction> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return transactions.stream()
                .filter(t -> !t.getTimestamp().isBefore(from) && !t.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }
}
