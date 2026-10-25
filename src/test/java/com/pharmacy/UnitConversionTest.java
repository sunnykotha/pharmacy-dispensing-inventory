package com.pharmacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnitConversionTest {
    @Test
    void convertsTabletsStripsAndBoxesToTablets() {
        assertEquals(12, UnitConversion.toTablets(12, UnitConversion.Unit.TABLET, 10, 5));
        assertEquals(30, UnitConversion.toTablets(3, UnitConversion.Unit.STRIP, 10, 5));
        assertEquals(100, UnitConversion.toTablets(2, UnitConversion.Unit.BOX, 10, 5));
    }

    @Test
    void convertsTabletsBackToWholePackUnits() {
        assertEquals(3, UnitConversion.fromTablets(30, UnitConversion.Unit.STRIP, 10, 5));
        assertEquals(2, UnitConversion.fromTablets(100, UnitConversion.Unit.BOX, 10, 5));
    }

    @Test
    void rejectsPartialStripOrBoxConversions() {
        assertThrows(IllegalArgumentException.class,
                () -> UnitConversion.fromTablets(25, UnitConversion.Unit.STRIP, 10, 5));
        assertThrows(IllegalArgumentException.class,
                () -> UnitConversion.fromTablets(30, UnitConversion.Unit.BOX, 10, 5));
    }

    @Test
    void rejectsInvalidQuantitiesAndPackSizes() {
        assertThrows(IllegalArgumentException.class,
                () -> UnitConversion.toTablets(-1, UnitConversion.Unit.TABLET, 10, 5));
        assertThrows(IllegalArgumentException.class,
                () -> UnitConversion.toTablets(1, UnitConversion.Unit.BOX, 0, 5));
    }
}