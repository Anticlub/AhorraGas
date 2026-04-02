package com.example.ahorragas.model;

public class Vehicle {
    private String name;
    private FuelType fuelType;
    private double consumption; // L/100km — 0 significa "no especificado"
    private double tankCapacity; // Litros — 0 significa "no especificado"

    public Vehicle() {
        this.name = "";
        this.fuelType = FuelType.GASOLEO_A;
        this.consumption = 0.0;
        this.tankCapacity = 0.0;
    }

    public Vehicle(String name, FuelType fuelType, double consumption, double tankCapacity) {
        this.name = name != null ? name : "";
        this.fuelType = fuelType != null ? fuelType : FuelType.GASOLEO_A;
        this.consumption = consumption >= 0 ? consumption : 0.0;
        this.tankCapacity = tankCapacity >= 0 ? tankCapacity : 0.0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType != null ? fuelType : FuelType.GASOLEO_A; }

    public double getConsumption() { return consumption; }
    public void setConsumption(double consumption) { this.consumption = consumption >= 0 ? consumption : 0.0; }

    public double getTankCapacity() { return tankCapacity; }
    public void setTankCapacity(double tankCapacity) { this.tankCapacity = tankCapacity >= 0 ? tankCapacity : 0.0; }

    /** True si el usuario ha introducido un consumo válido. */
    public boolean hasConsumption() { return consumption > 0; }

    /** True si el usuario ha introducido una capacidad de depósito válida. */
    public boolean hasTankCapacity() { return tankCapacity > 0; }

    /** Coste estimado para recorrer distanceKm con precioLitro €/L.
     *  Devuelve null si no hay consumo especificado. */
    public Double estimateCost(double distanceKm, double precioLitro) {
        if (!hasConsumption()) return null;
        return (consumption / 100.0) * distanceKm * precioLitro;
    }

    /**
     * Coste estimado de llenar el depósito con precioLitro €/L.
     * Devuelve null si no hay capacidad especificada.
     *
     * @param precioLitro Precio por litro en euros.
     * @return Coste total en euros o null si falta dato.
     */
    public Double estimateFillCost(double precioLitro) {
        if (!hasTankCapacity()) return null;
        return tankCapacity * precioLitro;
    }
}