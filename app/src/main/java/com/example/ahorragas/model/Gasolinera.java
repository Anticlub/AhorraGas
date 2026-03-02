package com.example.ahorragas.model;

import java.util.EnumMap;
import java.util.Map;

public class Gasolinera {
    private int id;
    private String marca;
    private String direccion;
    private String municipio;
    private Double lat, lon, distanceMeters;

    // ✅ Precios por combustible
    private final Map<FuelType, Double> precios = new EnumMap<>(FuelType.class);

    // CLASE BASE GASOLINERA (COMPARAR CON JSON DE API PARA VER SI NECESITA O LE SOBRAN ATRIBUTOS)
    public Gasolinera() {
    }

    public Gasolinera(int id, String marca, String municipio, String direccion,
                      Double lat, Double lon, Double precio) {
        this.id = id;
        this.marca = marca;
        this.municipio = municipio;
        this.direccion = direccion;
        this.lat = lat;
        this.lon = lon;

        // ✅ Compatibilidad: el "precio" antiguo lo tratamos como Gasóleo A por defecto
        setPrecio(FuelType.GASOLEO_A, precio);
    }

    // ======================
    // GETTERS / SETTERS BASE
    // ======================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    // ======================
    // ✅ PRECIOS POR COMBUSTIBLE
    // ======================

    public void setPrecio(FuelType fuel, Double value) {
        if (fuel == null) return;

        // Guardamos tal cual; si viene null, queda null (no rompe UI)
        precios.put(fuel, value);
    }

    public Double getPrecio(FuelType fuel) {
        if (fuel == null) return null;
        return precios.get(fuel);
    }

    /**
     * ✅ Compatibilidad con tu código actual:
     * si alguien llama getPrecio() sin fuel, devolvemos Gasóleo A.
     */
    public Double getPrecio() {
        return getPrecio(FuelType.GASOLEO_A);
    }

    /**
     * ✅ Compatibilidad: setPrecio "antiguo" = Gasóleo A.
     */
    public void setPrecio(Double value) {
        setPrecio(FuelType.GASOLEO_A, value);
    }
}