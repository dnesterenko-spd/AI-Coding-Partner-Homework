package com.support.domain;

public enum Status {
    NEW("New"),
    IN_PROGRESS("In Progress"),
    WAITING_CUSTOMER("Waiting for Customer"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}