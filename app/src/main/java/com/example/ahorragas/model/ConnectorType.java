package com.example.ahorragas.model;

/**
 * Tipos de conector eléctrico presentes en el XML de la DGT.
 * Mapea el valor técnico del XML a un nombre legible para el usuario.
 */
public enum ConnectorType {

    CCS2    ("iec62196T2COMBO", "CCS2",     "DC"),
    TYPE2   ("iec62196T2",      "Tipo 2",   "AC"),
    TYPE1   ("iec62196T1",      "Tipo 1",   "AC"),
    CHADEMO ("chademo",         "CHAdeMO",  "DC"),
    SCHUKO  ("domestic-f",      "Schuko",   "AC"),
    UNKNOWN ("",                "Desconocido", "");

    private final String xmlValue;
    private final String displayName;
    private final String currentType; // AC o DC

    ConnectorType(String xmlValue, String displayName, String currentType) {
        this.xmlValue    = xmlValue;
        this.displayName = displayName;
        this.currentType = currentType;
    }

    public String getDisplayName()  { return displayName; }
    public String getCurrentType()  { return currentType; }

    /**
     * Mapea el string técnico del XML de la DGT al enum correspondiente.
     *
     * @param value valor de egi:connectorType en el XML
     * @return ConnectorType correspondiente, o UNKNOWN si no se reconoce
     */
    public static ConnectorType fromXmlValue(String value) {
        if (value == null || value.trim().isEmpty()) return UNKNOWN;
        for (ConnectorType c : values()) {
            if (c.xmlValue.equalsIgnoreCase(value.trim())) return c;
        }
        return UNKNOWN;
    }
}