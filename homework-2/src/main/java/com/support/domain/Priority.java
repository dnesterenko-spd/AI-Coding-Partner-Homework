package com.support.domain;

public enum Priority {
    URGENT("Urgent"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}