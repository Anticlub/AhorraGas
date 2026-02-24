package com.example.ahorragas.network;

public class NumberUtils {

    public static Double parseSpanishDouble(String value) {
        if (value == null) return null;
        value = value.trim();
        if (value.isEmpty()) return null;

        value = value.replace(",", ".");

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }
}