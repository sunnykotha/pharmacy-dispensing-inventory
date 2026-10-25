package com.pharmacy;

import java.util.List;

public record WorkflowDispenseResult(
        boolean completed,
        String reason,
        List<DispenseEvent> events) {

    public WorkflowDispenseResult {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Result reason is required");
        }
        events = List.copyOf(events);
        if (!completed && !events.isEmpty()) {
            throw new IllegalArgumentException(
                    "A blocked workflow cannot return dispense events");
        }
    }

    public static WorkflowDispenseResult blocked(String reason) {
        return new WorkflowDispenseResult(false, reason, List.of());
    }

    public static WorkflowDispenseResult completed(
            String reason, List<DispenseEvent> events) {
        return new WorkflowDispenseResult(true, reason, events);
    }
}
