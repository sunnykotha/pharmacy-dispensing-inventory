package com.pharmacy;

public final class UnitConversion {
    public enum Unit {
        TABLET, STRIP, BOX
    }

    private UnitConversion() {}

    public static int toTablets(int quantity, Unit unit,
                                int tabletsPerStrip, int stripsPerBox) {
        validate(quantity, tabletsPerStrip, stripsPerBox);
        if (unit == null) {
            throw new IllegalArgumentException("Unit is required");
        }

        return switch (unit) {
            case TABLET -> quantity;
            case STRIP -> Math.multiplyExact(quantity, tabletsPerStrip);
            case BOX -> Math.multiplyExact(
                    Math.multiplyExact(quantity, stripsPerBox), tabletsPerStrip);
        };
    }

    public static int fromTablets(int tablets, Unit unit,
                                  int tabletsPerStrip, int stripsPerBox) {
        validate(tablets, tabletsPerStrip, stripsPerBox);
        if (unit == null) {
            throw new IllegalArgumentException("Unit is required");
        }

        int tabletsPerUnit = switch (unit) {
            case TABLET -> 1;
            case STRIP -> tabletsPerStrip;
            case BOX -> Math.multiplyExact(tabletsPerStrip, stripsPerBox);
        };

        if (tablets % tabletsPerUnit != 0) {
            throw new IllegalArgumentException(
                    "Tablet quantity cannot be represented as whole " + unit + " units");
        }
        return tablets / tabletsPerUnit;
    }

    private static void validate(int quantity, int tabletsPerStrip, int stripsPerBox) {
        if (quantity < 0 || tabletsPerStrip <= 0 || stripsPerBox <= 0) {
            throw new IllegalArgumentException("Quantity must be nonnegative and pack sizes positive");
        }
    }
}