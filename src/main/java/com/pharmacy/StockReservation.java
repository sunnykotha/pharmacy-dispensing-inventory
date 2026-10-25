package com.pharmacy;

import java.util.List;

public record StockReservation(
        String prescriptionId,
        int requestedUnits,
        List<BatchAllocation> allocations) {

    public StockReservation {
        if (prescriptionId == null || prescriptionId.isBlank()) {
            throw new IllegalArgumentException("Prescription ID is required");
        }
        if (requestedUnits <= 0) {
            throw new IllegalArgumentException("Requested units must be positive");
        }
        allocations = List.copyOf(allocations);
    }

    public int reservedUnits() {
        return allocations.stream().mapToInt(BatchAllocation::units).sum();
    }

    public int remainingUnits() {
        return requestedUnits - reservedUnits();
    }

    public boolean isComplete() {
        return remainingUnits() == 0;
    }
}