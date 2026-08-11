package com.ahorragas.app.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FuelTypeTest {

    @Test
    public void fromString_validName_returnsMatchingEnum() {
        assertEquals(FuelType.GASOLEO_A, FuelType.fromString("GASOLEO_A"));
        assertEquals(FuelType.ELECTRICO, FuelType.fromString("ELECTRICO"));
    }

    @Test
    public void fromString_nullOrInvalid_defaultsToGasoleoA() {
        assertEquals(FuelType.GASOLEO_A, FuelType.fromString(null));
        assertEquals(FuelType.GASOLEO_A, FuelType.fromString("no_existe"));
        assertEquals(FuelType.GASOLEO_A, FuelType.fromString(""));
    }

    @Test
    public void fromPrecioilId_known_returnsMatchingEnum() {
        assertEquals(FuelType.GASOLEO_A, FuelType.fromPrecioilId(6));
        assertEquals(FuelType.ELECTRICO, FuelType.fromPrecioilId(-1));
    }

    @Test
    public void fromPrecioilId_unknown_returnsNull() {
        assertNull(FuelType.fromPrecioilId(9999));
    }

    @Test
    public void displayName_isTheDescription() {
        assertEquals("Diesel", FuelType.GASOLEO_A.displayName());
        assertEquals("Eléctrico", FuelType.ELECTRICO.displayName());
    }
}
