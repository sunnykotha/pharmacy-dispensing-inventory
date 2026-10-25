package com.pharmacy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionTest {

    private final LocalDate today = LocalDate.of(2026, 10, 25);

    private Drug drug() {
        return new Drug("D001", "Example medicine", "tablet");
    }

    private Prescriber prescriber() {
        return new Prescriber(
                "P001",
                "Dr Example",
                "DEA-EXAMPLE",
                today.minusYears(1),
                today.plusYears(1));
    }

    private Prescription prescription(
            int authorizedRefills, int refillsUsed) {
        return new Prescription(
                "RX001",
                drug(),
                prescriber(),
                ControlledSubstanceSchedule.SCHEDULE_III,
                30,
                30,
                today,
                authorizedRefills,
                refillsUsed);
    }

    @Test
    void createsPrescriptionWithExpectedDetails() {
        Prescription rx = prescription(3, 1);

        assertEquals("RX001", rx.id());
        assertEquals(30, rx.quantity());
        assertEquals(30, rx.daysSupply());
        assertEquals(1, rx.refillsUsed());
        assertTrue(rx.hasRefillsRemaining());
    }

    @Test
    void reportsWhenNoRefillsRemain() {
        assertFalse(prescription(2, 2).hasRefillsRemaining());
        assertFalse(prescription(0, 0).hasRefillsRemaining());
    }

    @Test
    void validatesPrescriberRegistrationDateBoundaries() {
        Prescriber p = prescriber();

        assertTrue(p.isDeaRegistrationValidOn(today.minusYears(1)));
        assertTrue(p.isDeaRegistrationValidOn(today.plusYears(1)));
        assertFalse(p.isDeaRegistrationValidOn(today.minusYears(1).minusDays(1)));
        assertFalse(p.isDeaRegistrationValidOn(today.plusYears(1).plusDays(1)));
    }

    @Test
    void rejectsInvalidPrescriberData() {
        assertThrows(IllegalArgumentException.class,
                () -> new Prescriber(" ", "Doctor", "DEA",
                        today, today.plusDays(1)));

        assertThrows(IllegalArgumentException.class,
                () -> new Prescriber("P1", "Doctor", " ",
                        today, today.plusDays(1)));

        assertThrows(IllegalArgumentException.class,
                () -> new Prescriber("P1", "Doctor", "DEA",
                        today.plusDays(2), today));
    }

    @Test
    void rejectsInvalidPrescriptionQuantityAndDaysSupply() {
        assertThrows(IllegalArgumentException.class,
                () -> new Prescription("RX", drug(), prescriber(),
                        ControlledSubstanceSchedule.NON_CONTROLLED,
                        0, 30, today, 0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new Prescription("RX", drug(), prescriber(),
                        ControlledSubstanceSchedule.NON_CONTROLLED,
                        10, 0, today, 0, 0));
    }

    @Test
    void rejectsNegativeOrExcessiveRefillCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> prescription(-1, 0));

        assertThrows(IllegalArgumentException.class,
                () -> prescription(2, -1));

        assertThrows(IllegalArgumentException.class,
                () -> prescription(2, 3));
    }

    @Test
    void rejectsMissingRequiredPrescriptionFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new Prescription(" ", drug(), prescriber(),
                        ControlledSubstanceSchedule.NON_CONTROLLED,
                        10, 10, today, 0, 0));

        assertThrows(NullPointerException.class,
                () -> new Prescription("RX", null, prescriber(),
                        ControlledSubstanceSchedule.NON_CONTROLLED,
                        10, 10, today, 0, 0));
    }

    @Test
    void supportsAllSubstanceSchedules() {
        assertEquals(5, ControlledSubstanceSchedule.values().length);
    }
}