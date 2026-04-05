package com.example.ahorragas.model;

/**
 * Representa un plan de descuento de combustible publicado por el
 * Ministerio de Transición Ecológica (Geoportal Gasolineras).
 * Modelo de dominio puro, sin dependencias Android.
 */
public class PromotionPlan {

    /** Tipos de descuento que puede ofrecer un plan. */
    public enum DiscountType {
        CENTS_PER_LITER,   // Céntimos de euro en cada litro
        PERCENTAGE,        // Porcentaje sobre el precio total
        OTHER              // Cualquier otro formato no reconocido
    }

    private final String operator;
    private final String planName;
    private final String description;
    private final String validityDate;
    private final double discountValue;
    private final DiscountType discountType;
    private final String recipient;

    /**
     * @param operator      Nombre del operador (ej: "BP OIL ESPAÑA S.A.U.")
     * @param planName      Nombre del plan (ej: "Programa miBP")
     * @param description   Descripción larga del plan
     * @param validityDate  Fecha de validez o cadena vacía si no aplica
     * @param discountValue Cifra de descuento (ej: 3.0, 8.0)
     * @param discountType  Tipo de descuento
     * @param recipient     Destinatario (ej: "Todos los consumidores")
     */
    public PromotionPlan(String operator,
                         String planName,
                         String description,
                         String validityDate,
                         double discountValue,
                         DiscountType discountType,
                         String recipient) {
        this.operator      = operator      != null ? operator.trim()      : "";
        this.planName      = planName      != null ? planName.trim()      : "";
        this.description   = description   != null ? description.trim()   : "";
        this.validityDate  = validityDate  != null ? validityDate.trim()  : "";
        this.discountValue = discountValue >= 0 ? discountValue : 0.0;
        this.discountType  = discountType  != null ? discountType : DiscountType.OTHER;
        this.recipient     = recipient     != null ? recipient.trim()     : "";
    }

    public String getOperator()      { return operator; }
    public String getPlanName()      { return planName; }
    public String getDescription()   { return description; }
    public String getValidityDate()  { return validityDate; }
    public double getDiscountValue() { return discountValue; }
    public DiscountType getDiscountType() { return discountType; }
    public String getRecipient()     { return recipient; }
}