package com.example.ahorragas.model;

/**
 * Representa un registro de precio histórico de una estación
 * para un tipo de combustible en una fecha determinada.
 */
public class PriceHistoryEntry {

    private final FuelType fuelType;
    private final double price;
    private final String timestamp;

    /**
     * @param fuelType  tipo de combustible
     * @param price     precio en euros
     * @param timestamp fecha ISO 8601 (ej: "2026-04-01T23:30:15.000Z")
     */
    public PriceHistoryEntry(FuelType fuelType, double price, String timestamp) {
        this.fuelType = fuelType;
        this.price = price;
        this.timestamp = timestamp;
    }

    public FuelType getFuelType() { return fuelType; }
    public double getPrice() { return price; }
    public String getTimestamp() { return timestamp; }

    /**
     * Extrae solo la parte de fecha del timestamp (los 10 primeros caracteres).
     * Ejemplo: "2026-04-01T23:30:15.000Z" → "2026-04-01"
     *
     * @return fecha en formato yyyy-MM-dd
     */
    public String getDate() {
        if (timestamp == null || timestamp.length() < 10) return "";
        return timestamp.substring(0, 10);
    }
}