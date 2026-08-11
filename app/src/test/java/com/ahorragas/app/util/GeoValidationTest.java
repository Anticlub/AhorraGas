package com.ahorragas.app.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeoValidationTest {

    @Test
    public void validCoordinates_areAccepted() {
        assertTrue(GeoValidation.isValidLatLon(40.4168, -3.7038)); // Madrid
    }

    @Test
    public void boundaries_areAccepted() {
        assertTrue(GeoValidation.isValidLatLon(90.0, 180.0));
        assertTrue(GeoValidation.isValidLatLon(-90.0, -180.0));
    }

    @Test
    public void nulls_areRejected() {
        assertFalse(GeoValidation.isValidLatLon(null, -3.0));
        assertFalse(GeoValidation.isValidLatLon(40.0, null));
        assertFalse(GeoValidation.isValidLatLon(null, null));
    }

    @Test
    public void outOfRange_isRejected() {
        assertFalse(GeoValidation.isValidLatLon(90.1, 0.0));
        assertFalse(GeoValidation.isValidLatLon(-90.1, 0.0));
        assertFalse(GeoValidation.isValidLatLon(0.5, 180.1));
        assertFalse(GeoValidation.isValidLatLon(0.5, -180.1));
    }

    @Test
    public void zeroZero_isRejected() {
        // Caso típico de dato roto (isla nula).
        assertFalse(GeoValidation.isValidLatLon(0.0, 0.0));
    }

    @Test
    public void nanAndInfinity_areRejected() {
        assertFalse(GeoValidation.isValidLatLon(Double.NaN, -3.0));
        assertFalse(GeoValidation.isValidLatLon(40.0, Double.NaN));
        assertFalse(GeoValidation.isValidLatLon(Double.POSITIVE_INFINITY, -3.0));
    }
}
