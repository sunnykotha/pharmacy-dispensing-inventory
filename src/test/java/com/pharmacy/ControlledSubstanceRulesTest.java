package com.pharmacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ControlledSubstanceRulesTest {
    private final LocalDate today = LocalDate.of(2026, 10, 25);
    private Drug drug;
    private Prescriber validPrescriber;
    private ControlledSubstanceRules rules;

    @BeforeEach
    void setUp() {
        drug = new Drug("D001", "Example medicine", "tablet");
        validPrescriber = new Prescriber(
                "P001", "Dr Example", "DEA-EXAMPLE",
                today.minusYears(1), today.plusYears(1));
        rules = ControlledSubstanceRules.usFederalBaseline();
    }

    private Prescription rx(
            ControlledSubstanceSchedule schedule,
            int daysSupply,
            int authorizedRefills,
            int refillsUsed,
            LocalDate issueDate,
            Prescriber prescriber) {
        return new Prescription(
                "RX001", drug, prescriber, schedule,
                30, daysSupply, issueDate,
                authorizedRefills, refillsUsed);
    }

    @Test
    void allowsScheduleTwoWithoutRefills() {
        RuleDecision result = rules.validate(
                rx(ControlledSubstanceSchedule.SCHEDULE_II,
                        30, 0, 0, today, validPrescriber), today);

        assertTrue(result.allowed());
    }

    @Test
    void blocksScheduleTwoWithRefills() {
        Prescription invalid = rx(
                ControlledSubstanceSchedule.SCHEDULE_II,
                30, 1, 0, today, validPrescriber);

        assertFalse(rules.validate(invalid, today).allowed());
    }

    @Test
    void allowsScheduleThreeWithFiveRefills() {
        Prescription prescription = rx(
                ControlledSubstanceSchedule.SCHEDULE_III,
                30, 5, 0, today, validPrescriber);

        assertTrue(rules.validate(prescription, today).allowed());
    }

    @Test
    void blocksMoreThanFiveRefills() {
        Prescription prescription = rx(
                ControlledSubstanceSchedule.SCHEDULE_IV,
                30, 6, 0, today, validPrescriber);

        assertFalse(rules.validate(prescription, today).allowed());
    }

    @Test
    void blocksExpiredDeaRegistration() {
        Prescriber expired = new Prescriber(
                "P2", "Dr Expired", "DEA-OLD",
                today.minusYears(2), today.minusDays(1));

        Prescription prescription = rx(
                ControlledSubstanceSchedule.SCHEDULE_III,
                30, 2, 0, today, expired);

        RuleDecision result = rules.validate(prescription, today);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("DEA"));
    }

    @Test
    void acceptsDeaRegistrationOnFinalValidDate() {
        Prescriber endingToday = new Prescriber(
                "P3", "Dr Valid", "DEA-VALID",
                today.minusYears(1), today);

        Prescription prescription = rx(
                ControlledSubstanceSchedule.SCHEDULE_III,
                30, 2, 0, today, endingToday);

        assertTrue(rules.validate(prescription, today).allowed());
    }

    @Test
    void blocksRefillOutsideSixMonthWindow() {
        LocalDate issued = today.minusMonths(6).minusDays(1);

        Prescription prescription = rx(
                ControlledSubstanceSchedule.SCHEDULE_III,
                30, 2, 1, issued, validPrescriber);

        assertFalse(rules.validate(prescription, today).allowed());
    }

    @Test
    void allowsRefillOnSixMonthWindowBoundary() {
        LocalDate issued = today.minusMonths(6);

        Prescription prescription = rx(
                ControlledSubstanceSchedule.SCHEDULE_III,
                30, 2, 1, issued, validPrescriber);

        assertTrue(rules.validate(prescription, today).allowed());
    }

    @Test
    void supportsConfiguredMaximumDaysSupply() {
        Map<ControlledSubstanceSchedule, SchedulePolicy> policies =
                new EnumMap<>(ControlledSubstanceSchedule.class);

        policies.put(ControlledSubstanceSchedule.SCHEDULE_II,
                new SchedulePolicy(0, Period.ofDays(1), 30));
        SchedulePolicy other =
                new SchedulePolicy(5, Period.ofMonths(6), 30);
        policies.put(ControlledSubstanceSchedule.SCHEDULE_III, other);
        policies.put(ControlledSubstanceSchedule.SCHEDULE_IV, other);
        policies.put(ControlledSubstanceSchedule.SCHEDULE_V, other);

        ControlledSubstanceRules configured =
                new ControlledSubstanceRules(policies);

        Prescription tooLong = rx(
                ControlledSubstanceSchedule.SCHEDULE_II,
                31, 0, 0, today, validPrescriber);

        assertFalse(configured.validate(tooLong, today).allowed());
    }

    @Test
    void allowsNonControlledPrescriptionWithoutDeaValidation() {
        Prescriber expired = new Prescriber(
                "P2", "Dr Expired", "DEA-OLD",
                today.minusYears(2), today.minusDays(1));

        Prescription prescription = rx(
                ControlledSubstanceSchedule.NON_CONTROLLED,
                30, 0, 0, today, expired);

        assertTrue(rules.validate(prescription, today).allowed());
    }

    @Test
    void blocksEarlyRefillAtTwentyDaysForThirtyDaySupply() {
        RuleDecision result = rules.validateEarlyRefill(
                today.minusDays(20), 30, today, null);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("24 days required"));
    }

    @Test
    void allowsRefillAtTwentyFourDaysForThirtyDaySupply() {
        RuleDecision result = rules.validateEarlyRefill(
                today.minusDays(24), 30, today, null);

        assertTrue(result.allowed());
    }

    @Test
    void pharmacistOverrideRequiresAReason() {
        RuleDecision noReason = rules.validateEarlyRefill(
                today.minusDays(20), 30, today, " ");

        RuleDecision withReason = rules.validateEarlyRefill(
                today.minusDays(20), 30, today,
                "Travel exception reviewed by pharmacist");

        assertFalse(noReason.allowed());
        assertTrue(withReason.allowed());
        assertTrue(withReason.reason().contains("Travel exception"));
    }

    @Test
    void rejectsInvalidEarlyRefillInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> rules.validateEarlyRefill(
                        today, 0, today, null));

        assertThrows(IllegalArgumentException.class,
                () -> rules.validateEarlyRefill(
                        today, 30, today.minusDays(1), null));
    }
}