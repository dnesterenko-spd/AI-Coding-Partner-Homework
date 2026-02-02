package com.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSummaryResponse {
    private String accountId;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private Integer transactionCount;
    private LocalDateTime mostRecentTransactionDate;

    public TransactionSummaryResponse() {
    }

    public TransactionSummaryResponse(String accountId, BigDecimal totalDeposits, BigDecimal totalWithdrawals,
                                     Integer transactionCount, LocalDateTime mostRecentTransactionDate) {
        this.accountId = accountId;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.transactionCount = transactionCount;
        this.mostRecentTransactionDate = mostRecentTransactionDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(BigDecimal totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public LocalDateTime getMostRecentTransactionDate() {
        return mostRecentTransactionDate;
    }

    public void setMostRecentTransactionDate(LocalDateTime mostRecentTransactionDate) {
        this.mostRecentTransactionDate = mostRecentTransactionDate;
    }
}
