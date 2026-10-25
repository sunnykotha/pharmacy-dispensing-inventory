package com.pharmacy;

public record RuleDecision(boolean allowed, String reason) {
    public RuleDecision {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Decision reason is required");
        }
    }

    public static RuleDecision allow(String reason) {
        return new RuleDecision(true, reason);
    }

    public static RuleDecision block(String reason) {
        return new RuleDecision(false, reason);
    }
}