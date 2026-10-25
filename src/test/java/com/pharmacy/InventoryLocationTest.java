package com.pharmacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryLocationTest {
    private final LocalDate today = LocalDate.of(2026, 10, 25);
    private Drug drug;
    private InventoryLocation location;

    @BeforeEach
    void setUp() {
        drug = new Drug("D001", "Paracetamol", "tablet");
        location = new InventoryLocation("LOC1", "Main Pharmacy");
    }

    private Batch batch(String lot, int units, int expiryDays) {
        return new Batch(lot, drug, today.plusDays(expiryDays), units);
    }

    @Test
    void reservesStockWithoutDecrementingOnHand() {
        location.addBatch(batch("A", 30, 60));

        StockReservation reservation =
                location.reserve("RX1", drug, 20, today, today.plusDays(10));

        assertEquals(20, reservation.reservedUnits());
        assertEquals(30, location.onHandUnits("D001"));
        assertEquals(20, location.reservedUnits("D001"));
        assertEquals(10, location.availableUnits("D001"));
    }

    @Test
    void twoPrescriptionsCannotReserveTheSameUnits() {
        location.addBatch(batch("A", 30, 60));

        StockReservation first =
                location.reserve("RX1", drug, 20, today, today.plusDays(10));
        StockReservation second =
                location.reserve("RX2", drug, 20, today, today.plusDays(10));

        assertEquals(20, first.reservedUnits());
        assertEquals(10, second.reservedUnits());
        assertEquals(10, second.remainingUnits());
        assertEquals(0, location.availableUnits("D001"));
    }

    @Test
    void reservationUsesFefoAcrossBatches() {
        location.addBatch(batch("B", 100, 240));
        location.addBatch(batch("A", 30, 60));

        StockReservation reservation =
                location.reserve("RX1", drug, 60, today, today.plusDays(30));

        assertEquals(List.of(
                new BatchAllocation("A", 30),
                new BatchAllocation("B", 30)), reservation.allocations());
    }

    @Test
    void releasesReservationBackToAvailableStock() {
        location.addBatch(batch("A", 30, 60));
        location.reserve("RX1", drug, 20, today, today.plusDays(10));

        location.releaseReservation("RX1");

        assertEquals(0, location.reservedUnits("D001"));
        assertEquals(30, location.availableUnits("D001"));
    }

    @Test
    void rejectsDuplicateActiveReservation() {
        location.addBatch(batch("A", 30, 60));
        location.reserve("RX1", drug, 10, today, today.plusDays(10));

        assertThrows(IllegalArgumentException.class,
                () -> location.reserve(
                        "RX1", drug, 10, today, today.plusDays(10)));
    }

    @Test
    void partialReservationCannotBeDispensedAsComplete() {
        location.addBatch(batch("A", 5, 60));
        location.reserve("RX1", drug, 10, today, today.plusDays(5));

        assertThrows(IllegalStateException.class,
                () -> location.dispenseReserved("RX1", today));

        assertEquals(5, location.onHandUnits("D001"));
    }

    @Test
    void dispensingCreatesAuditEventsAndDecrementsStock() {
        location.addBatch(batch("A", 10, 60));
        location.reserve("RX1", drug, 7, today, today.plusDays(5));

        List<DispenseEvent> events =
                location.dispenseReserved("RX1", today);

        assertEquals(1, events.size());
        assertEquals("RX1", events.get(0).prescriptionId());
        assertEquals("LOC1", events.get(0).inventoryLocationId());
        assertEquals("A", events.get(0).lotNumber());
        assertEquals(7, events.get(0).units());
        assertEquals(3, location.onHandUnits("D001"));
        assertEquals(0, location.reservedUnits("D001"));
        assertEquals(1, location.dispenseEvents().size());
    }

    @Test
    void recalledBatchCannotBeDispensedAfterReservation() {
        Batch batch = batch("A", 10, 60);
        location.addBatch(batch);
        location.reserve("RX1", drug, 5, today, today.plusDays(5));

        batch.recall();

        assertThrows(IllegalStateException.class,
                () -> location.dispenseReserved("RX1", today));
        assertEquals(10, location.onHandUnits("D001"));
        assertTrue(location.dispenseEvents().isEmpty());
    }

    @Test
    void excludesExpiredAndQuarantinedStockFromReservations() {
        Batch expired = batch("OLD", 10, -1);
        Batch quarantined = batch("HOLD", 10, 30);
        quarantined.quarantine();
        location.addBatch(expired);
        location.addBatch(quarantined);

        StockReservation reservation =
                location.reserve("RX1", drug, 5, today, today.plusDays(5));

        assertEquals(0, reservation.reservedUnits());
        assertEquals(5, reservation.remainingUnits());
    }

    @Test
    void rejectsReservationWithInvalidQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> location.reserve(
                        "RX1", drug, 0, today, today));
    }
}