package com.pharmacy;

import java.util.Objects;

public record BatchAllocation(String lotNumber, int units) {
    public BatchAllocation {
        if (lotNumber == null || lotNumber.isBlank()) {
            throw new IllegalArgumentException("Lot number is required");
        }
        if (units <= 0) {
            throw new IllegalArgumentException("Allocated units must be positive");
        }
        lotNumber = lotNumber.trim();
    }
}