package com.example.ahorragas.model;

/**
 * Rango de precios calculado a partir de un conjunto de gasolineras.
 * min/max pueden ser null si no hay datos válidos.
 */
public class PriceRange {
    private final Double min;
    private final Double max;
    private final int count;

    public PriceRange(Double min, Double max, int count) {
        this.min = min;
        this.max = max;
        this.count = count;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public int getCount() {
        return count;
    }

    public boolean isEmpty() {
        return count <= 0 || min == null || max == null;
    }
}