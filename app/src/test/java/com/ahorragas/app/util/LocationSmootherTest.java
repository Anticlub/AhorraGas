package com.ahorragas.app.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocationSmootherTest {

    @Test
    public void firstSample_returnsSameValue() {
        LocationSmoother s = new LocationSmoother();
        double[] r = s.add(40.0, -3.0);
        assertEquals(40.0, r[0], 1e-9);
        assertEquals(-3.0, r[1], 1e-9);
    }

    @Test
    public void averagesSamplesWithinWindow() {
        LocationSmoother s = new LocationSmoother();
        s.add(40.0, -3.0);
        double[] r = s.add(42.0, -1.0);
        // media de las dos muestras
        assertEquals(41.0, r[0], 1e-9);
        assertEquals(-2.0, r[1], 1e-9);
    }

    @Test
    public void windowSlidesAndDropsOldest() {
        LocationSmoother s = new LocationSmoother(2);
        s.add(10.0, 10.0);
        s.add(20.0, 20.0);
        // con ventana 2, la tercera muestra desplaza a la primera
        double[] r = s.add(30.0, 30.0);
        assertEquals(25.0, r[0], 1e-9); // media de 20 y 30
        assertEquals(25.0, r[1], 1e-9);
    }

    @Test
    public void windowOfOne_alwaysReturnsLatest() {
        LocationSmoother s = new LocationSmoother(1);
        s.add(1.0, 1.0);
        double[] r = s.add(5.0, 9.0);
        assertEquals(5.0, r[0], 1e-9);
        assertEquals(9.0, r[1], 1e-9);
    }
}
