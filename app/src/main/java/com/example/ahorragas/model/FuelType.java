package com.example.ahorragas.model;

public enum FuelType {
    GASOLINA_95_E5("Precio Gasolina 95 E5", "Gasolina 95", 10),
    GASOLINA_95_E5_PREMIUM("Precio Gasolina 95 E5 Premium", "Gasolina 95 sin proteccion", 11),
    GASOLINA_98_E5("Precio Gasolina 98 E5", "Gasolina 98", 13),
    GASOLEO_A("Precio Gasoleo A", "DIesel", 6),
    GASOLEO_PREMIUM("Precio Gasoleo Premium", "Diesel mejorado", 8),
    GASOLEO_B("Precio Gasoleo B", "Gasoleo B", 7),
    GASOLEO_C("Precio Gasoleo C", "Gasoleo C", 15),
    BIODIESEL("Precio Biodiesel", "Biodiesel", 1),
    BIOETANOL("Precio Bioetanol", "Bioetanol", 2),
    GLP("Precio Gases licuados del petróleo", "GLP (Gas Licuado de Petróleo)", 5),
    GNC("Precio Gas Natural Comprimido", "GNC (Gas Natural Comprimision)", 3),
    ELECTRICO("", "Eléctrico", -1);

    private final String apiKey;
    private final String displayName;
    private final int precioilId;

    FuelType(String apiKey, String displayName, int precioilId) {
        this.apiKey = apiKey;
        this.displayName = displayName;
        this.precioilId = precioilId;
    }

    public String apiKey() {
        return apiKey;
    }

    public String displayName() {
        return displayName;
    }
    /**
     * Devuelve el idFuelType de la API de Precioil.
     * Devuelve -1 para ELECTRICO (no tiene histórico de precios).
     *
     * @return id del tipo de combustible en la API de Precioil
     */
    public int precioilId() {
        return precioilId;
    }

    /**
     * Devuelve el FuelType correspondiente a un idFuelType de Precioil.
     *
     * @param id idFuelType de la API de Precioil
     * @return FuelType correspondiente o null si no se encuentra
     */
    public static FuelType fromPrecioilId(int id) {
        for (FuelType ft : values()) {
            if (ft.precioilId == id) return ft;
        }
        return null;
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