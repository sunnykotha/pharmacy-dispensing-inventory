package com.pharmacy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class InventoryLocation {
    private final String id;
    private final String name;
    private final Map<String, Batch> batches = new LinkedHashMap<>();
    private final Map<String, StockReservation> reservations = new HashMap<>();
    private final List<DispenseEvent> events = new ArrayList<>();

    public InventoryLocation(String id, String name) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Inventory location ID and name are required");
        }
        this.id = id.trim();
        this.name = name.trim();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public synchronized void addBatch(Batch batch) {
        Objects.requireNonNull(batch, "Batch is required");
        if (batches.containsKey(batch.lotNumber())) {
            throw new IllegalArgumentException(
                    "Duplicate lot number in this location: "
                            + batch.lotNumber());
        }
        batches.put(batch.lotNumber(), batch);
    }

    public synchronized int onHandUnits(String drugId) {
        return batches.values().stream()
                .filter(b -> b.drug().id().equals(drugId))
                .mapToInt(Batch::quantity)
                .sum();
    }

    public synchronized int reservedUnits(String drugId) {
        int total = 0;
        for (StockReservation reservation : reservations.values()) {
            for (BatchAllocation allocation : reservation.allocations()) {
                Batch batch = batches.get(allocation.lotNumber());
                if (batch != null && batch.drug().id().equals(drugId)) {
                    total += allocation.units();
                }
            }
        }
        return total;
    }

    public synchronized int availableUnits(String drugId) {
        return onHandUnits(drugId) - reservedUnits(drugId);
    }

    public synchronized StockReservation reserve(
            String prescriptionId,
            Drug drug,
            int requestedUnits,
            LocalDate today,
            LocalDate courseEnd) {

        if (prescriptionId == null || prescriptionId.isBlank()) {
            throw new IllegalArgumentException("Prescription ID is required");
        }
        Objects.requireNonNull(drug, "Drug is required");
        Objects.requireNonNull(today, "Today is required");
        Objects.requireNonNull(courseEnd, "Course end is required");

        if (requestedUnits <= 0) {
            throw new IllegalArgumentException("Requested units must be positive");
        }
        if (courseEnd.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Course end cannot be before today");
        }
        if (reservations.containsKey(prescriptionId)) {
            throw new IllegalArgumentException(
                    "Prescription already has an active reservation");
        }

        Map<String, Integer> reservedByLot = new HashMap<>();
        for (StockReservation existing : reservations.values()) {
            for (BatchAllocation allocation : existing.allocations()) {
                reservedByLot.merge(
                        allocation.lotNumber(), allocation.units(), Integer::sum);
            }
        }

        List<Batch> eligible = batches.values().stream()
                .filter(b -> b.drug().id().equals(drug.id()))
                .filter(b -> b.isUsableForCourse(today, courseEnd))
                .sorted(Comparator.comparing(Batch::expiryDate)
                        .thenComparing(Batch::lotNumber))
                .toList();

        int remaining = requestedUnits;
        List<BatchAllocation> allocations = new ArrayList<>();

        for (Batch batch : eligible) {
            if (remaining == 0) {
                break;
            }

            int alreadyReserved = reservedByLot.getOrDefault(
                    batch.lotNumber(), 0);
            int free = batch.quantity() - alreadyReserved;
            int take = Math.min(remaining, free);

            if (take > 0) {
                allocations.add(new BatchAllocation(batch.lotNumber(), take));
                remaining -= take;
            }
        }

        StockReservation reservation = new StockReservation(
                prescriptionId, requestedUnits, allocations);
        reservations.put(prescriptionId, reservation);
        return reservation;
    }

    public synchronized void releaseReservation(String prescriptionId) {
        if (reservations.remove(prescriptionId) == null) {
            throw new IllegalArgumentException(
                    "No active reservation for prescription: " + prescriptionId);
        }
    }

    public synchronized List<DispenseEvent> dispenseReserved(String prescriptionId, LocalDate dispenseDate) {

        return dispenseReserved(prescriptionId, dispenseDate, dispenseDate);

    }


    public synchronized List<DispenseEvent> dispenseReserved(String prescriptionId, LocalDate dispenseDate, LocalDate courseEnd) {
        Objects.requireNonNull(dispenseDate, "Dispense date is required");

        StockReservation reservation = reservations.get(prescriptionId);
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "No active reservation for prescription: " + prescriptionId);
        }
        if (!reservation.isComplete()) {
            throw new IllegalStateException(
                    "Cannot complete dispensing: reservation is partial");
        }

        // Validate every allocation before decrementing any stock.
        for (BatchAllocation allocation : reservation.allocations()) {
            Batch batch = batches.get(allocation.lotNumber());
            if (batch == null
                    || !batch.isUsableForCourse(dispenseDate, courseEnd)
                    || batch.quantity() < allocation.units()) {
                throw new IllegalStateException(
                        "Reserved batch is no longer eligible: "
                                + allocation.lotNumber());
            }
        }

        List<DispenseEvent> created = new ArrayList<>();
        for (BatchAllocation allocation : reservation.allocations()) {
            Batch batch = batches.get(allocation.lotNumber());
            batch.removeStock(allocation.units());

            created.add(new DispenseEvent(
                    UUID.randomUUID().toString(),
                    prescriptionId,
                    id,
                    batch.drug().id(),
                    batch.lotNumber(),
                    allocation.units(),
                    dispenseDate));
        }

        events.addAll(created);
        reservations.remove(prescriptionId);
        return List.copyOf(created);
    }

    public synchronized List<DispenseEvent> dispenseEvents() {
        return List.copyOf(events);
    }

    public synchronized int usableAvailableUnits(String drugId, java.time.LocalDate today) {
        if (drugId == null || drugId.isBlank() || today == null) {
            throw new IllegalArgumentException("Drug ID and date are required");
        }
        java.util.Map<String, Integer> reservedByLot = new java.util.HashMap<>();
        for (StockReservation reservation : reservations.values()) {
            for (BatchAllocation allocation : reservation.allocations()) {
                Batch batch = batches.get(allocation.lotNumber());
                if (batch.drug().id().equals(drugId)) {
                    reservedByLot.merge(batch.lotNumber(), allocation.units(), Integer::sum);
                }
            }
        }
        return batches.values().stream()
            .filter(b -> b.drug().id().equals(drugId))
            .filter(b -> b.isUsableForCourse(today, today))
            .mapToInt(b -> Math.max(0, b.quantity()
                - reservedByLot.getOrDefault(b.lotNumber(), 0)))
            .sum();
    }
}