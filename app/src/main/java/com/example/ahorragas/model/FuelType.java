package com.example.ahorragas.model;

public enum FuelType {
    GASOLINA_95_E5("Precio Gasolina 95 E5", "Gasolina 95"),
    GASOLINA_95_E5_PREMIUM("Precio Gasolina 95 E5 Premium", "Gasolina 95 sin proteccion"),
    GASOLINA_98_E5("Precio Gasolina 98 E5", "Gasolina 98"),
    GASOLEO_A("Precio Gasoleo A", "DIesel"),
    GASOLEO_PREMIUM("Precio Gasoleo Premium", "Diesel mejorado"),
    GASOLEO_B("Precio Gasoleo B", "Gasoleo B"),
    GASOLEO_C("Precio Gasoleo C", "Gasoleo C"),
    BIODIESEL("Precio Biodiesel", "Biodiesel"),
    BIOETANOL("Precio Bioetanol", "Bioetanol"),
    GLP("Precio Gases licuados del petróleo", "GLP (Gas Licuado de Petróleo)"),
    GNC("Precio Gas Natural Comprimido", "GNC (Gas Natural Comprimision)"),
    ELECTRICO("", "Eléctrico");

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