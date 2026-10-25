package com.pharmacy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class FefoDispensingService {

    public DispenseResult dispense(
            Drug requestedDrug,
            List<Batch> batches,
            int requestedUnits,
            LocalDate today,
            LocalDate courseEnd) {

        Objects.requireNonNull(requestedDrug, "Requested drug is required");
        Objects.requireNonNull(batches, "Batches are required");
        Objects.requireNonNull(today, "Today is required");
        Objects.requireNonNull(courseEnd, "Course end is required");

        if (requestedUnits <= 0) {
            throw new IllegalArgumentException(
                    "Requested units must be positive");
        }
        if (courseEnd.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Course end cannot be before today");
        }

        List<Batch> eligible = batches.stream()
                .filter(Objects::nonNull)
                .filter(batch -> batch.drug().id().equals(requestedDrug.id()))
                .filter(batch -> batch.isUsableForCourse(today, courseEnd))
                .sorted(Comparator.comparing(Batch::expiryDate)
                        .thenComparing(Batch::lotNumber))
                .toList();

        int remaining = requestedUnits;
        List<BatchAllocation> allocations = new ArrayList<>();

        for (Batch batch : eligible) {
            if (remaining == 0) {
                break;
            }

            int take = Math.min(remaining, batch.quantity());
            if (take > 0) {
                batch.removeStock(take);
                allocations.add(new BatchAllocation(batch.lotNumber(), take));
                remaining -= take;
            }
        }

        return new DispenseResult(
                requestedUnits,
                requestedUnits - remaining,
                remaining,
                allocations);
    }
}