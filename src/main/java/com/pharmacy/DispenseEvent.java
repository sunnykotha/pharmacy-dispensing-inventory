package com.pharmacy;

import java.time.LocalDate;
import java.util.Objects;

public record DispenseEvent(
        String eventId,
        String prescriptionId,
        String inventoryLocationId,
        String drugId,
        String lotNumber,
        int units,
        LocalDate dispenseDate) {

    public DispenseEvent {
        if (eventId == null || eventId.isBlank()
                || prescriptionId == null || prescriptionId.isBlank()
                || inventoryLocationId == null || inventoryLocationId.isBlank()
                || drugId == null || drugId.isBlank()
                || lotNumber == null || lotNumber.isBlank()) {
            throw new IllegalArgumentException("Event identifiers are required");
        }
        if (units <= 0) {
            throw new IllegalArgumentException("Dispensed units must be positive");
        }
        Objects.requireNonNull(dispenseDate, "Dispense date is required");
    }
}