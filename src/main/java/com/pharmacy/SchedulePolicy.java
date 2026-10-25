package com.pharmacy;

import java.time.Period;

public record SchedulePolicy(
        int maximumRefills,
        Period refillWindow,
        int maximumDaysSupply) {

    public SchedulePolicy {
        if (maximumRefills < 0) {
            throw new IllegalArgumentException(
                    "Maximum refills cannot be negative");
        }
        if (refillWindow == null || refillWindow.isNegative()
                || refillWindow.isZero()) {
            throw new IllegalArgumentException(
                    "Refill window must be positive");
        }
        if (maximumDaysSupply <= 0) {
            throw new IllegalArgumentException(
                    "Maximum days supply must be positive");
        }
    }
}