package com.ahorragas.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeoUtilsTest {

    private static final double DELTA = 1.0; // metros

    @Test
    public void distanceMeters_samePoint_isZero() {
        assertEquals(0.0, GeoUtils.distanceMeters(40.0, -3.0, 40.0, -3.0), 0.0001);
    }

    @Test
    public void distanceMeters_oneDegreeLatitude_isAbout111km() {
        // 1° de latitud ≈ 111.195 m con R = 6371 km.
        double d = GeoUtils.distanceMeters(0.0, 0.0, 1.0, 0.0);
        assertEquals(111194.93, d, DELTA);
    }

    @Test
    public void distanceMeters_isSymmetric() {
        double ab = GeoUtils.distanceMeters(40.4168, -3.7038, 41.3874, 2.1686);
        double ba = GeoUtils.distanceMeters(41.3874, 2.1686, 40.4168, -3.7038);
        assertEquals(ab, ba, 0.0001);
    }

    @Test
    public void distanceMeters_madridBarcelona_isAbout505km() {
        double d = GeoUtils.distanceMeters(40.4168, -3.7038, 41.3874, 2.1686);
        // Distancia real ~505 km; margen amplio para no ser frágil.
        assertTrue("esperaba ~505 km pero fue " + d, d > 495000 && d < 515000);
    }

    @Test
    public void boundingBox_containsCenterAndIsSymmetric() {
        double lat = 40.0, lon = -3.0, radius = 1000.0;
        double[] box = GeoUtils.boundingBox(lat, lon, radius); // [minLat, maxLat, minLon, maxLon]

        assertTrue(box[0] < lat && lat < box[1]); // lat dentro
        assertTrue(box[2] < lon && lon < box[3]); // lon dentro

        // Simétrico respecto al centro.
        assertEquals(box[1] - lat, lat - box[0], 1e-9);
        assertEquals(box[3] - lon, lon - box[2], 1e-9);
    }
}
