package com.pharmacy;

import java.time.LocalDate;
import java.util.Objects;

public record Prescription(
        String id,
        Drug drug,
        Prescriber prescriber,
        ControlledSubstanceSchedule schedule,
        int quantity,
        int daysSupply,
        LocalDate issueDate,
        int authorizedRefills,
        int refillsUsed) {

    public Prescription {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Prescription ID is required");
        }
        Objects.requireNonNull(drug, "Drug is required");
        Objects.requireNonNull(prescriber, "Prescriber is required");
        Objects.requireNonNull(schedule, "Schedule is required");
        Objects.requireNonNull(issueDate, "Issue date is required");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Prescription quantity must be positive");
        }
        if (daysSupply <= 0) {
            throw new IllegalArgumentException(
                    "Days supply must be positive");
        }
        if (authorizedRefills < 0) {
            throw new IllegalArgumentException(
                    "Authorized refills cannot be negative");
        }
        if (refillsUsed < 0 || refillsUsed > authorizedRefills) {
            throw new IllegalArgumentException(
                    "Refills used must be between zero and authorized refills");
        }

        id = id.trim();
    }

    public boolean hasRefillsRemaining() {
        return refillsUsed < authorizedRefills;
    }
}