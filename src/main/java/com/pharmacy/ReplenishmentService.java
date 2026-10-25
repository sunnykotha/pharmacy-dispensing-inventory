package com.pharmacy;

import java.time.LocalDate;

public class ReplenishmentService {

    public PurchaseSuggestion suggest(
            InventoryLocation location,
            Drug drug,
            LocalDate today,
            int reorderPoint,
            int targetStock) {

        if (location == null || drug == null || today == null
                || reorderPoint < 0 || targetStock <= reorderPoint) {
            throw new IllegalArgumentException("Invalid replenishment inputs");
        }

        int available = location.usableAvailableUnits(drug.id(), today);
        int suggested = available <= reorderPoint ? targetStock - available : 0;

        return new PurchaseSuggestion(
                drug.id(), location.id(), available, reorderPoint,
                targetStock, suggested, today);
    }
}
