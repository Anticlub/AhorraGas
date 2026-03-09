package com.example.ahorragas.model;

/**
 * Tipos de combustible soportados por la app.
 * Cada uno mapea a la clave exacta del JSON del Ministerio.
 */
public enum FuelType {
    GASOLEO_A("Precio Gasoleo A", "Gasóleo A"),
    GASOLINA_95_E5("Precio Gasolina 95 E5", "Gasolina 95"),
    GASOLINA_98_E5("Precio Gasolina 98 E5", "Gasolina 98");

    private final String apiKey;
    private final String displayName;

    FuelType(String apiKey, String displayName) {
        this.apiKey = apiKey;
        this.displayName = displayName;
    }

    public String apiKey() {
        return apiKey;
    }

    public String displayName() {
        return displayName;
    }

    public static FuelType fromString(String value) {
        if (value == null) return GASOLEO_A;
        try {
            return FuelType.valueOf(value);
        } catch (Exception ignore) {
            return GASOLEO_A;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
