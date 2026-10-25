package com.pharmacy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class DispensingWorkflow {
    private final ControlledSubstanceRules rules;

    public DispensingWorkflow(ControlledSubstanceRules rules) {
        this.rules = Objects.requireNonNull(rules, "Rules are required");
    }

    public WorkflowDispenseResult dispense(
            Prescription prescription,
            InventoryLocation location,
            LocalDate dispensingDate) {

        Objects.requireNonNull(prescription, "Prescription is required");
        Objects.requireNonNull(location, "Inventory location is required");
        Objects.requireNonNull(dispensingDate, "Dispensing date is required");

        RuleDecision decision = rules.validate(prescription, dispensingDate);
        if (!decision.allowed()) {
            return WorkflowDispenseResult.blocked(decision.reason());
        }

        LocalDate courseEnd = dispensingDate.plusDays(
                prescription.daysSupply() - 1L);

        StockReservation reservation = location.reserve(
                prescription.id(),
                prescription.drug(),
                prescription.quantity(),
                dispensingDate,
                courseEnd);

        if (!reservation.isComplete()) {
            location.releaseReservation(prescription.id());
            return WorkflowDispenseResult.blocked(
                    "Insufficient eligible stock to fill prescription");
        }

        try {
            List<DispenseEvent> events =
                    location.dispenseReserved(prescription.id(), dispensingDate, courseEnd);
            return WorkflowDispenseResult.completed(
                    "Prescription validated and dispensed", events);
        } catch (IllegalStateException ex) {
            location.releaseReservation(prescription.id());
            return WorkflowDispenseResult.blocked(
                    "Dispensing stopped: " + ex.getMessage());
        }
    }
}
