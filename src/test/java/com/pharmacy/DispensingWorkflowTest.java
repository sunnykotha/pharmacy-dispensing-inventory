package com.pharmacy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DispensingWorkflowTest {
    private final LocalDate today = LocalDate.of(2026, 10, 25);
    private final Drug drug = new Drug("D1", "Example medicine", "tablet");
    private final ControlledSubstanceRules rules =
            ControlledSubstanceRules.usFederalBaseline();
    private final DispensingWorkflow workflow = new DispensingWorkflow(rules);

    private Prescriber prescriber(LocalDate validFrom, LocalDate validThrough) {
        return new Prescriber("P1", "Example Prescriber", "DEA123",
                validFrom, validThrough);
    }

    private Prescription prescription(
            ControlledSubstanceSchedule schedule, int quantity,
            Prescriber prescriber) {
        return new Prescription("RX1", drug, prescriber, schedule,
                quantity, 7, today, 0, 0);
    }

    private InventoryLocation location(int units) {
        InventoryLocation location = new InventoryLocation("L1", "Main pharmacy");
        location.addBatch(new Batch("LOT1", drug, today.plusMonths(6), units));
        return location;
    }

    @Test
    void validPrescriptionDispensesAndCreatesEvents() {
        InventoryLocation location = location(20);
        var result = workflow.dispense(
                prescription(ControlledSubstanceSchedule.NON_CONTROLLED, 8,
                        prescriber(today.minusYears(1), today.plusYears(1))),
                location, today);

        assertTrue(result.completed());
        assertEquals(1, result.events().size());
        assertEquals(8, result.events().get(0).units());
        assertEquals(12, location.onHandUnits("D1"));
        assertEquals(1, location.dispenseEvents().size());
    }

    @Test
    void invalidControlledPrescriptionDoesNotTouchInventory() {
        InventoryLocation location = location(20);
        var result = workflow.dispense(
                prescription(ControlledSubstanceSchedule.SCHEDULE_III, 8,
                        prescriber(today.minusYears(2), today.minusDays(1))),
                location, today);

        assertFalse(result.completed());
        assertTrue(result.events().isEmpty());
        assertEquals(20, location.onHandUnits("D1"));
        assertEquals(0, location.dispenseEvents().size());
        assertEquals(0, location.reservedUnits("D1"));
    }

    @Test
    void insufficientStockReleasesPartialReservation() {
        InventoryLocation location = location(4);
        var result = workflow.dispense(
                prescription(ControlledSubstanceSchedule.NON_CONTROLLED, 8,
                        prescriber(today.minusYears(1), today.plusYears(1))),
                location, today);

        assertFalse(result.completed());
        assertTrue(result.reason().contains("Insufficient eligible stock"));
        assertTrue(result.events().isEmpty());
        assertEquals(4, location.onHandUnits("D1"));
        assertEquals(0, location.reservedUnits("D1"));
        assertTrue(location.dispenseEvents().isEmpty());
    }

    @Test
    void successfulDispensingRemovesReservation() {
        InventoryLocation location = location(10);
        var result = workflow.dispense(
                prescription(ControlledSubstanceSchedule.NON_CONTROLLED, 5,
                        prescriber(today.minusYears(1), today.plusYears(1))),
                location, today);

        assertTrue(result.completed());
        assertEquals(0, location.reservedUnits("D1"));
        assertEquals(5, location.onHandUnits("D1"));
    }

    @Test
    void expiredStockCannotCompleteWorkflow() {
        InventoryLocation location = new InventoryLocation("L1", "Main pharmacy");
        location.addBatch(new Batch("OLD", drug, today.minusDays(1), 20));

        var result = workflow.dispense(
                prescription(ControlledSubstanceSchedule.NON_CONTROLLED, 5,
                        prescriber(today.minusYears(1), today.plusYears(1))),
                location, today);

        assertFalse(result.completed());
        assertEquals(20, location.onHandUnits("D1"));
        assertEquals(0, location.reservedUnits("D1"));
        assertTrue(location.dispenseEvents().isEmpty());
    }
}
