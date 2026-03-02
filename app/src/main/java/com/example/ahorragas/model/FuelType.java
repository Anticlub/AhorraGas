package com.example.ahorragas.model;

/**
 * Tipos de combustible soportados en la app.
 * Cada uno mapea a la clave exacta que viene en el JSON del ministerio.
 */
public enum FuelType {
    GASOLEO_A("Precio Gasoleo A"),
    GASOLINA_95_E5("Precio Gasolina 95 E5"),
    GASOLINA_98_E5("Precio Gasolina 98 E5");

    private final String apiKey;

    FuelType(String apiKey) {
        this.apiKey = apiKey;
    }

    public String apiKey() {
        return apiKey;
    }

    /**
     * Convierte un String guardado (por ejemplo en prefs) a FuelType con fallback.
     */
    public static FuelType fromString(String value) {
        if (value == null) return GASOLEO_A;
        try {
            return FuelType.valueOf(value);
        } catch (Exception ignore) {
            return GASOLEO_A;
        }
    }
}