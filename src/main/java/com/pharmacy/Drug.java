package com.pharmacy;

import java.util.Objects;

public record Drug(String id, String name, String inventoryUnit) {
    public Drug {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Drug ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Drug name is required");
        }
        if (inventoryUnit == null || inventoryUnit.isBlank()) {
            throw new IllegalArgumentException("Inventory unit is required");
        }

        id = id.trim();
        name = name.trim();
        inventoryUnit = inventoryUnit.trim();
    }
}