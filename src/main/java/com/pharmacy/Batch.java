package com.pharmacy;

import java.time.LocalDate;
import java.util.Objects;

public final class Batch {
    private final String lotNumber;
    private final Drug drug;
    private final LocalDate expiryDate;
    private int quantity;
    private boolean recalled;
    private boolean quarantined;

    public Batch(
            String lotNumber,
            Drug drug,
            LocalDate expiryDate,
            int quantity) {
        if (lotNumber == null || lotNumber.isBlank()) {
            throw new IllegalArgumentException("Lot number is required");
        }
        this.drug = Objects.requireNonNull(drug, "Drug is required");
        this.expiryDate = Objects.requireNonNull(
                expiryDate, "Expiry date is required");
        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "Batch quantity cannot be negative");
        }

        this.lotNumber = lotNumber.trim();
        this.quantity = quantity;
    }

    public String lotNumber() {
        return lotNumber;
    }

    public Drug drug() {
        return drug;
    }

    public LocalDate expiryDate() {
        return expiryDate;
    }

    public synchronized int quantity() {
        return quantity;
    }

    public synchronized boolean isRecalled() {
        return recalled;
    }

    public synchronized boolean isQuarantined() {
        return quarantined;
    }

    public synchronized void recall() {
        recalled = true;
    }

    public synchronized void quarantine() {
        quarantined = true;
    }

    public synchronized void releaseFromQuarantine() {
        quarantined = false;
    }

    public synchronized boolean isUsableForCourse(
            LocalDate today, LocalDate courseEnd) {
        Objects.requireNonNull(today, "Today is required");
        Objects.requireNonNull(courseEnd, "Course end is required");

        if (courseEnd.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Course end cannot be before today");
        }

        return quantity > 0
                && !recalled
                && !quarantined
                && !expiryDate.isBefore(today)
                && !expiryDate.isBefore(courseEnd);
    }

    public synchronized void removeStock(int units) {
        if (units <= 0) {
            throw new IllegalArgumentException(
                    "Units to remove must be positive");
        }
        if (units > quantity) {
            throw new IllegalStateException(
                    "Insufficient batch stock");
        }
        quantity -= units;
    }
}