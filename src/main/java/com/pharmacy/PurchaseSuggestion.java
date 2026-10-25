package com.pharmacy;

import java.time.LocalDate;

public record PurchaseSuggestion(
        String drugId,
        String inventoryLocationId,
        int usableAvailableUnits,
        int reorderPoint,
        int targetStock,
        int suggestedUnits,
        LocalDate evaluatedOn) {

    public PurchaseSuggestion {
        if (drugId == null || drugId.isBlank()
                || inventoryLocationId == null || inventoryLocationId.isBlank()
                || usableAvailableUnits < 0 || reorderPoint < 0
                || targetStock <= reorderPoint || suggestedUnits < 0
                || evaluatedOn == null) {
            throw new IllegalArgumentException("Invalid purchase suggestion");
        }
    }

    public boolean purchaseNeeded() {
        return suggestedUnits > 0;
    }
}
