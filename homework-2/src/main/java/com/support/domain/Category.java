package com.support.domain;

public enum Category {
    ACCOUNT_ACCESS("Account Access"),
    TECHNICAL_ISSUE("Technical Issue"),
    BILLING_QUESTION("Billing Question"),
    FEATURE_REQUEST("Feature Request"),
    BUG_REPORT("Bug Report"),
    OTHER("Other");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}