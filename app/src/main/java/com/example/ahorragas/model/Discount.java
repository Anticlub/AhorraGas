package com.example.ahorragas.model;

/**
 * Representa un descuento de combustible asociado a una marca de gasolinera.
 */
public class Discount {

    public enum Type {
        CENTS_PER_LITER,  // Descuento en céntimos por litro (ej: 5 = 0.05 €/L)
        PERCENTAGE        // Descuento en porcentaje (ej: 5%)
    }

    private String brandName;
    private Type type;
    private double value;

    public Discount() {
        this.brandName = "";
        this.type = Type.CENTS_PER_LITER;
        this.value = 0.0;
    }

    public Discount(String brandName, Type type, double value) {
        this.brandName = brandName != null ? brandName : "";
        this.type = type != null ? type : Type.CENTS_PER_LITER;
        this.value = value >= 0 ? value : 0.0;
    }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName != null ? brandName : ""; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type != null ? type : Type.CENTS_PER_LITER; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value >= 0 ? value : 0.0; }

    /**
     * Aplica el descuento a un precio por litro dado.
     * Para céntimos/litro, el valor introducido es en céntimos (ej: 5 = 0.05 €/L).
     *
     * @param pricePerLiter Precio original por litro en euros.
     * @return Precio con descuento aplicado, nunca negativo.
     */
    public double applyTo(double pricePerLiter) {
        double discounted;
        if (type == Type.PERCENTAGE) {
            discounted = pricePerLiter * (1.0 - value / 100.0);
        } else {
            // value está en céntimos, convertir a euros
            discounted = pricePerLiter - (value / 100.0);
        }
        return Math.max(0.0, discounted);
    }

    /**
     * Comprueba si este descuento aplica a la marca de gasolinera dada.
     *
     * @param stationBrand Marca de la gasolinera.
     * @return true si el nombre de marca coincide (insensible a mayúsculas).
     */
    public boolean appliesTo(String stationBrand) {
        if (stationBrand == null || brandName.isEmpty()) return false;
        return stationBrand.trim().toLowerCase().contains(brandName.trim().toLowerCase());
    }
}