package com.pharmacy;

import java.time.LocalDate;

public record Prescriber(
        String id,
        String name,
        String deaRegistrationNumber,
        LocalDate deaValidFrom,
        LocalDate deaValidThrough) {

    public Prescriber {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Prescriber ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Prescriber name is required");
        }
        if (deaRegistrationNumber == null
                || deaRegistrationNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "DEA registration number is required");
        }
        if (deaValidFrom == null || deaValidThrough == null) {
            throw new IllegalArgumentException(
                    "DEA registration validity dates are required");
        }
        if (deaValidThrough.isBefore(deaValidFrom)) {
            throw new IllegalArgumentException(
                    "DEA validity end cannot precede its start");
        }

        id = id.trim();
        name = name.trim();
        deaRegistrationNumber = deaRegistrationNumber.trim();
    }

    public boolean isDeaRegistrationValidOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        return !date.isBefore(deaValidFrom)
                && !date.isAfter(deaValidThrough);
    }
}