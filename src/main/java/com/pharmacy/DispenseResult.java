package com.pharmacy;

import java.util.List;

public record DispenseResult(
        int requestedUnits,
        int dispensedUnits,
        int remainingUnits,
        List<BatchAllocation> allocations) {

    public DispenseResult {
        if (requestedUnits <= 0) {
            throw new IllegalArgumentException("Requested units must be positive");
        }
        if (dispensedUnits < 0 || remainingUnits < 0
                || dispensedUnits + remainingUnits != requestedUnits) {
            throw new IllegalArgumentException("Invalid dispense totals");
        }
        allocations = List.copyOf(allocations);
    }

    public boolean isPartial() {
        return dispensedUnits > 0 && remainingUnits > 0;
    }

    public boolean isComplete() {
        return remainingUnits == 0;
    }
}