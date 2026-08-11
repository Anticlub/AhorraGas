package com.ahorragas.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NumberUtilsTest {

    @Test
    public void parsesCommaDecimal() {
        assertEquals(1.234, NumberUtils.parseSpanishDouble("1,234"), 1e-9);
    }

    @Test
    public void parsesDotDecimal() {
        assertEquals(1.234, NumberUtils.parseSpanishDouble("1.234"), 1e-9);
    }

    @Test
    public void trimsWhitespace() {
        assertEquals(2.5, NumberUtils.parseSpanishDouble("  2,5  "), 1e-9);
    }

    @Test
    public void nullOrEmpty_returnsNull() {
        assertNull(NumberUtils.parseSpanishDouble(null));
        assertNull(NumberUtils.parseSpanishDouble(""));
        assertNull(NumberUtils.parseSpanishDouble("   "));
    }

    @Test
    public void invalid_returnsNull() {
        assertNull(NumberUtils.parseSpanishDouble("abc"));
        assertNull(NumberUtils.parseSpanishDouble("1,5€"));
    }
}
