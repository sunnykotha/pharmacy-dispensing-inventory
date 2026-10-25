package com.pharmacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    private Drug drug;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        drug = new Drug("D001", "Paracetamol", "tablet");
        today = LocalDate.of(2026, 10, 25);
    }

    @Test
    void applicationReturnsExpectedMessage() {
        assertEquals("Pharmacy Dispensing and Inventory",
                new App().message());
    }

    @Test
    void createsDrugAndBatch() {
        Batch batch = new Batch("LOT-A", drug, today.plusMonths(2), 30);
        assertEquals("D001", batch.drug().id());
        assertEquals("LOT-A", batch.lotNumber());
        assertEquals(30, batch.quantity());
    }

    @Test
    void rejectsInvalidDrugAndBatchData() {
        assertThrows(IllegalArgumentException.class,
                () -> new Drug(" ", "Medicine", "tablet"));
        assertThrows(IllegalArgumentException.class,
                () -> new Drug("D1", "Medicine", " "));
        assertThrows(IllegalArgumentException.class,
                () -> new Batch("LOT", drug, today, -1));
        assertThrows(NullPointerException.class,
                () -> new Batch("LOT", null, today, 10));
    }

    @Test
    void acceptsBatchThatCoversEntireCourse() {
        Batch batch = new Batch("LOT-A", drug, today.plusDays(10), 30);
        assertTrue(batch.isUsableForCourse(today, today.plusDays(7)));
    }

    @Test
    void rejectsBatchExpiringBeforeCourseFinishes() {
        Batch batch = new Batch("LOT-A", drug, today.plusDays(5), 30);
        assertFalse(batch.isUsableForCourse(today, today.plusDays(7)));
    }

    @Test
    void rejectsExpiredRecalledAndQuarantinedBatches() {
        Batch expired = new Batch("OLD", drug, today.minusDays(1), 30);
        Batch recalled = new Batch("RECALL", drug, today.plusDays(30), 30);
        Batch quarantined = new Batch("HOLD", drug, today.plusDays(30), 30);
        recalled.recall();
        quarantined.quarantine();

        assertFalse(expired.isUsableForCourse(today, today));
        assertFalse(recalled.isUsableForCourse(today, today));
        assertFalse(quarantined.isUsableForCourse(today, today));
    }

    @Test
    void expiryOnCourseEndIsAccepted() {
        Batch batch = new Batch("BOUNDARY", drug, today.plusDays(7), 10);
        assertTrue(batch.isUsableForCourse(today, today.plusDays(7)));
    }

    @Test
    void rejectsStockRemovalThatIsInvalidOrExceedsQuantity() {
        Batch batch = new Batch("LOT-A", drug, today.plusDays(30), 10);
        assertThrows(IllegalArgumentException.class, () -> batch.removeStock(0));
        assertThrows(IllegalStateException.class, () -> batch.removeStock(11));
        batch.removeStock(4);
        assertEquals(6, batch.quantity());
    }

    @Test
    void emptyBatchCannotSupplyMedication() {
        Batch batch = new Batch("EMPTY", drug, today.plusDays(30), 0);
        assertFalse(batch.isUsableForCourse(today, today));
    }

    @Test
    void fefoSplitsSixtyTabletsAcrossEarliestExpiryBatches() {
        Batch a = new Batch("A", drug, today.plusMonths(2), 30);
        Batch b = new Batch("B", drug, today.plusMonths(8), 100);
        FefoDispensingService service = new FefoDispensingService();

        DispenseResult result = service.dispense(
                drug, List.of(b, a), 60, today, today.plusDays(30));

        assertEquals(60, result.dispensedUnits());
        assertEquals(0, result.remainingUnits());
        assertTrue(result.isComplete());
        assertEquals(List.of(
                new BatchAllocation("A", 30),
                new BatchAllocation("B", 30)), result.allocations());
        assertEquals(0, a.quantity());
        assertEquals(70, b.quantity());
    }

    @Test
    void fefoSkipsExpiredRecalledAndQuarantinedBatches() {
        Batch expired = new Batch("OLD", drug, today.minusDays(1), 20);
        Batch recalled = new Batch("R", drug, today.plusMonths(1), 20);
        Batch held = new Batch("Q", drug, today.plusMonths(2), 20);
        Batch valid = new Batch("GOOD", drug, today.plusMonths(3), 10);
        recalled.recall();
        held.quarantine();

        DispenseResult result = new FefoDispensingService().dispense(
                drug, List.of(expired, recalled, held, valid),
                15, today, today.plusDays(5));

        assertEquals(10, result.dispensedUnits());
        assertEquals(5, result.remainingUnits());
        assertTrue(result.isPartial());
        assertEquals(List.of(new BatchAllocation("GOOD", 10)),
                result.allocations());
        assertEquals(20, expired.quantity());
        assertEquals(20, recalled.quantity());
        assertEquals(20, held.quantity());
    }

    @Test
    void fefoRejectsBatchThatExpiresBeforeCourseEnds() {
        Batch tooEarly = new Batch("EARLY", drug, today.plusDays(5), 50);
        Batch suitable = new Batch("LATE", drug, today.plusDays(20), 20);

        DispenseResult result = new FefoDispensingService().dispense(
                drug, List.of(tooEarly, suitable), 10,
                today, today.plusDays(10));

        assertEquals(List.of(new BatchAllocation("LATE", 10)),
                result.allocations());
        assertEquals(50, tooEarly.quantity());
    }

    @Test
    void fefoDoesNotDispenseAnotherDrug() {
        Drug otherDrug = new Drug("D002", "Ibuprofen", "tablet");
        Batch other = new Batch("OTHER", otherDrug, today.plusMonths(2), 50);

        DispenseResult result = new FefoDispensingService().dispense(
                drug, List.of(other), 10, today, today.plusDays(2));

        assertEquals(0, result.dispensedUnits());
        assertEquals(10, result.remainingUnits());
        assertEquals(50, other.quantity());
    }

    @Test
    void fefoRejectsNonPositiveRequestedQuantity() {
        FefoDispensingService service = new FefoDispensingService();
        assertThrows(IllegalArgumentException.class,
                () -> service.dispense(drug, List.of(), 0, today, today));
    }

    @Test
    void fefoUsesLotNumberToBreakEqualExpiryDates() {
        Batch b = new Batch("B", drug, today.plusDays(10), 5);
        Batch a = new Batch("A", drug, today.plusDays(10), 5);

        DispenseResult result = new FefoDispensingService().dispense(
                drug, List.of(b, a), 6, today, today.plusDays(5));

        assertEquals(List.of(
                new BatchAllocation("A", 5),
                new BatchAllocation("B", 1)), result.allocations());
    }
}