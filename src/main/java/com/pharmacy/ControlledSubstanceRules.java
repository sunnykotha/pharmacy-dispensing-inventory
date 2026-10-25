package com.pharmacy;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ControlledSubstanceRules {
    private final Map<ControlledSubstanceSchedule, SchedulePolicy> policies;

    public ControlledSubstanceRules(
            Map<ControlledSubstanceSchedule, SchedulePolicy> configuredPolicies) {
        Objects.requireNonNull(configuredPolicies, "Policies are required");

        this.policies = new EnumMap<>(ControlledSubstanceSchedule.class);
        this.policies.putAll(configuredPolicies);

        for (ControlledSubstanceSchedule schedule
                : ControlledSubstanceSchedule.values()) {
            if (schedule != ControlledSubstanceSchedule.NON_CONTROLLED
                    && !this.policies.containsKey(schedule)) {
                throw new IllegalArgumentException(
                        "Missing policy for " + schedule);
            }
        }
    }

    public static ControlledSubstanceRules usFederalBaseline() {
        Map<ControlledSubstanceSchedule, SchedulePolicy> policies =
                new EnumMap<>(ControlledSubstanceSchedule.class);

        // Maximum days supply is deliberately configurable.
        // Integer.MAX_VALUE means no limit is imposed by this policy object.
        policies.put(ControlledSubstanceSchedule.SCHEDULE_II,
                new SchedulePolicy(0, Period.ofDays(1), Integer.MAX_VALUE));

        SchedulePolicy scheduleIIIToV =
                new SchedulePolicy(5, Period.ofMonths(6), Integer.MAX_VALUE);

        policies.put(ControlledSubstanceSchedule.SCHEDULE_III, scheduleIIIToV);
        policies.put(ControlledSubstanceSchedule.SCHEDULE_IV, scheduleIIIToV);
        policies.put(ControlledSubstanceSchedule.SCHEDULE_V, scheduleIIIToV);

        return new ControlledSubstanceRules(policies);
    }

    public RuleDecision validate(
            Prescription prescription, LocalDate dispensingDate) {
        Objects.requireNonNull(prescription, "Prescription is required");
        Objects.requireNonNull(dispensingDate, "Dispensing date is required");

        if (prescription.schedule()
                == ControlledSubstanceSchedule.NON_CONTROLLED) {
            return RuleDecision.allow("Non-controlled prescription");
        }

        if (!prescription.prescriber()
                .isDeaRegistrationValidOn(dispensingDate)) {
            return RuleDecision.block(
                    "Prescriber DEA registration is not valid on dispensing date");
        }

        SchedulePolicy policy = policies.get(prescription.schedule());

        if (prescription.daysSupply() > policy.maximumDaysSupply()) {
            return RuleDecision.block(
                    "Days supply exceeds configured schedule limit");
        }

        if (prescription.authorizedRefills() > policy.maximumRefills()) {
            return RuleDecision.block(
                    "Authorized refills exceed configured schedule limit");
        }

        if (prescription.schedule()
                == ControlledSubstanceSchedule.SCHEDULE_II
                && prescription.authorizedRefills() != 0) {
            return RuleDecision.block(
                    "Schedule II prescriptions cannot have refills");
        }

        if (prescription.schedule()
                != ControlledSubstanceSchedule.SCHEDULE_II
                && prescription.refillsUsed() > 0
                && dispensingDate.isAfter(
                        prescription.issueDate().plus(policy.refillWindow()))) {
            return RuleDecision.block(
                    "Prescription refill window has expired");
        }

        return RuleDecision.allow("Controlled-substance rules passed");
    }

    public RuleDecision validateEarlyRefill(
            LocalDate previousDispenseDate,
            int previousDaysSupply,
            LocalDate requestedDate,
            String pharmacistOverrideReason) {

        Objects.requireNonNull(
                previousDispenseDate, "Previous dispense date is required");
        Objects.requireNonNull(requestedDate, "Requested date is required");

        if (previousDaysSupply <= 0) {
            throw new IllegalArgumentException(
                    "Previous days supply must be positive");
        }
        if (requestedDate.isBefore(previousDispenseDate)) {
            throw new IllegalArgumentException(
                    "Requested date cannot precede previous dispense date");
        }

        long elapsedDays = ChronoUnit.DAYS.between(
                previousDispenseDate, requestedDate);

        long minimumElapsedDays =
                (long) Math.ceil(previousDaysSupply * 0.80);

        if (elapsedDays >= minimumElapsedDays) {
            return RuleDecision.allow(
                    "Early-refill threshold satisfied");
        }

        if (pharmacistOverrideReason != null
                && !pharmacistOverrideReason.isBlank()) {
            return RuleDecision.allow(
                    "Early refill permitted by pharmacist override: "
                            + pharmacistOverrideReason.trim());
        }

        return RuleDecision.block(
                "Early refill blocked: " + elapsedDays
                        + " of " + previousDaysSupply
                        + " days elapsed; at least "
                        + minimumElapsedDays + " days required");
    }
}