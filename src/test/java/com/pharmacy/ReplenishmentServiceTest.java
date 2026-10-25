package com.pharmacy;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReplenishmentServiceTest {
    private final LocalDate today = LocalDate.of(2026, 10, 25);
    private final Drug drug = new Drug("D1", "Example medicine", "tablet");
    private final ReplenishmentService service = new ReplenishmentService();

    private InventoryLocation location(int units) {
        InventoryLocation location = new InventoryLocation("L1", "Main pharmacy");
        location.addBatch(new Batch("LOT1", drug, today.plusMonths(6), units));
        return location;
    }

    @Test
    void suggestsNothingWhenStockIsAboveReorderPoint() {
        var result = service.suggest(location(30), drug, today, 10, 50);
        assertFalse(result.purchaseNeeded());
        assertEquals(0, result.suggestedUnits());
    }

    @Test
    void suggestsTargetMinusAvailableWhenAtReorderPoint() {
        var result = service.suggest(location(10), drug, today, 10, 50);
        assertTrue(result.purchaseNeeded());
        assertEquals(40, result.suggestedUnits());
    }

    @Test
    void suggestsTargetMinusAvailableWhenBelowReorderPoint() {
        var result = service.suggest(location(4), drug, today, 10, 50);
        assertEquals(46, result.suggestedUnits());
    }

    @Test
    void suggestsFullTargetWhenNoStockExists() {
        var result = service.suggest(location(0), drug, today, 10, 50);
        assertEquals(50, result.suggestedUnits());
    }

    @Test
    void excludesExpiredStock() {
        InventoryLocation location = new InventoryLocation("L1", "Main pharmacy");
        location.addBatch(new Batch("OLD", drug, today.minusDays(1), 100));
        var result = service.suggest(location, drug, today, 10, 50);
        assertEquals(0, result.usableAvailableUnits());
        assertEquals(50, result.suggestedUnits());
    }

    @Test
    void excludesRecalledStock() {
        InventoryLocation location = location(100);
        // Add a separate recalled lot.
        Batch recalled = new Batch("LOT2", drug, today.plusMonths(6), 20);
        recalled.recall();
        location.addBatch(recalled);
        var result = service.suggest(location, drug, today, 10, 50);
        assertEquals(100, result.usableAvailableUnits());
        assertEquals(0, result.suggestedUnits());
    }

    @Test
    void subtractsReservedUnitsFromUsableAvailableStock() {
        InventoryLocation location = location(30);
        var reservation = location.reserve("RX1", drug, 25, today, today.plusDays(7));
        var result = service.suggest(location, drug, today, 10, 50);
        assertEquals(5, result.usableAvailableUnits());
        assertEquals(45, result.suggestedUnits());
    }

    @Test
    void rejectsInvalidThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> service.suggest(location(20), drug, today, 10, 10));
        assertThrows(IllegalArgumentException.class,
                () -> service.suggest(location(20), drug, today, -1, 10));
    }
}
