package com.banking.validator;

import com.banking.dto.CreateTransactionRequest;
import com.banking.dto.ValidationErrorResponse;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for transaction creation requests.
 * Performs validation for amounts, currencies, account formats, and transaction types.
 */
@Component
public class TransactionValidator {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^ACC-[A-Za-z0-9]{5}$");
    private static final List<String> VALID_CURRENCIES = List.of(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "SEK", "NZD",
            "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "INR", "BRL", "ZAR"
    );

    public ValidationErrorResponse validate(CreateTransactionRequest request) {
        List<ValidationErrorResponse.ValidationError> errors = new ArrayList<>();

        // Validate amount
        if (request.getAmount() == null) {
            errors.add(new ValidationErrorResponse.ValidationError("amount", "Amount is required"));
        } else {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ValidationErrorResponse.ValidationError("amount", "Amount must be a positive number"));
            }
            // Strip trailing zeros before checking scale (handles numbers like 100.00 or 100E2)
            BigDecimal normalizedAmount = request.getAmount().stripTrailingZeros();
            if (normalizedAmount.scale() > 2) {
                errors.add(new ValidationErrorResponse.ValidationError("amount", "Amount must have maximum 2 decimal places"));
            }
        }

        // Validate currency
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            errors.add(new ValidationErrorResponse.ValidationError("currency", "Currency is required"));
        } else if (!VALID_CURRENCIES.contains(request.getCurrency().toUpperCase())) {
            errors.add(new ValidationErrorResponse.ValidationError("currency", "Invalid currency code: " + request.getCurrency()));
        }

        // Validate transaction type
        if (request.getType() == null || request.getType().isBlank()) {
            errors.add(new ValidationErrorResponse.ValidationError("type", "Transaction type is required"));
        } else {
            String type = request.getType().toUpperCase();
            if (!type.equals("DEPOSIT") && !type.equals("WITHDRAWAL") && !type.equals("TRANSFER")) {
                errors.add(new ValidationErrorResponse.ValidationError("type", "Transaction type must be DEPOSIT, WITHDRAWAL, or TRANSFER"));
            }
        }

        // Validate accounts based on type
        if (request.getType() != null) {
            String type = request.getType().toUpperCase();

            if (type.equals("TRANSFER")) {
                if (request.getFromAccount() == null || !isValidAccountNumber(request.getFromAccount())) {
                    errors.add(new ValidationErrorResponse.ValidationError("fromAccount", "From account must follow format ACC-XXXXX"));
                }
                if (request.getToAccount() == null || !isValidAccountNumber(request.getToAccount())) {
                    errors.add(new ValidationErrorResponse.ValidationError("toAccount", "To account must follow format ACC-XXXXX"));
                }
                if (request.getFromAccount() != null && request.getToAccount() != null &&
                    request.getFromAccount().equals(request.getToAccount())) {
                    errors.add(new ValidationErrorResponse.ValidationError("toAccount", "From and to accounts cannot be the same"));
                }
            } else if (type.equals("DEPOSIT")) {
                if (request.getToAccount() == null || !isValidAccountNumber(request.getToAccount())) {
                    errors.add(new ValidationErrorResponse.ValidationError("toAccount", "To account must follow format ACC-XXXXX"));
                }
            } else if (type.equals("WITHDRAWAL")) {
                if (request.getFromAccount() == null || !isValidAccountNumber(request.getFromAccount())) {
                    errors.add(new ValidationErrorResponse.ValidationError("fromAccount", "From account must follow format ACC-XXXXX"));
                }
            }
        }

        if (!errors.isEmpty()) {
            return new ValidationErrorResponse("Validation failed", errors);
        }
        return null;
    }

    private boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null && ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches();
    }
}
