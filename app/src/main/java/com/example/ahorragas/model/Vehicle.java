package com.example.ahorragas.model;

public class Vehicle {
    private String name;
    private FuelType fuelType;
    private double consumption; // L/100km — 0 significa "no especificado"

    public Vehicle() {
        this.name = "";
        this.fuelType = FuelType.GASOLEO_A;
        this.consumption = 0.0;
    }

    public Vehicle(String name, FuelType fuelType, double consumption) {
        this.name = name != null ? name : "";
        this.fuelType = fuelType != null ? fuelType : FuelType.GASOLEO_A;
        this.consumption = consumption >= 0 ? consumption : 0.0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType != null ? fuelType : FuelType.GASOLEO_A; }

    public double getConsumption() { return consumption; }
    public void setConsumption(double consumption) { this.consumption = consumption >= 0 ? consumption : 0.0; }

    /** True si el usuario ha introducido un consumo válido. */
    public boolean hasConsumption() { return consumption > 0; }

    /** Coste estimado para recorrer distanceKm con precioLitro €/L.
     *  Devuelve null si no hay consumo especificado. */
    public Double estimateCost(double distanceKm, double precioLitro) {
        if (!hasConsumption()) return null;
        return (consumption / 100.0) * distanceKm * precioLitro;
    }
}