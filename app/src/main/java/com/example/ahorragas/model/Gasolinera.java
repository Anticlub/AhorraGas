package com.example.ahorragas.model;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class Gasolinera {
    private int id;
    private String marca;
    private String direccion;
    private String municipio;
    private String horario;
    private Double lat;
    private Double lon;
    private Double distanceMeters;
    private PriceLevel priceLevel = PriceLevel.UNKNOWN;

    private final Map<FuelType, Double> precios = new EnumMap<>(FuelType.class);

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

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario == null ? null : horario.trim();
    }

    public String getFormattedHorario() {
        if (horario == null || horario.trim().isEmpty()) {
            return "Horario no disponible";
        }
        return horario.trim();
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

    public PriceLevel getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel != null ? priceLevel : PriceLevel.UNKNOWN;
    }

    public void setPrecio(FuelType fuel, Double value) {
        if (fuel == null) return;
        precios.put(fuel, value);
    }

    public Double getPrecio(FuelType fuel) {
        if (fuel == null) return null;
        return precios.get(fuel);
    }

    public Double getPrecio() {
        return getPrecio(FuelType.GASOLEO_A);
    }

    public void setPrecio(Double value) {
        setPrecio(FuelType.GASOLEO_A, value);
    }

    public String getBrandInitial() {
        String raw = marca != null ? marca.trim() : "";
        if (raw.isEmpty()) return "?";

        String cleaned = raw.replaceAll("[^\\p{L}\\p{N} ]", "").trim();
        if (cleaned.isEmpty()) cleaned = raw;

        String[] parts = cleaned.split("\\s+");
        if (parts.length >= 2) {
            String abbr = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.getDefault());
            return abbr.length() > 2 ? abbr.substring(0, 2) : abbr;
        }

        return cleaned.substring(0, Math.min(2, cleaned.length())).toUpperCase(Locale.getDefault());
    }

    public String getDisplayAddress() {
        String street = direccion != null ? direccion.trim() : "";
        String city = municipio != null ? municipio.trim() : "";

        if (street.isEmpty() && city.isEmpty()) return "Dirección no disponible";
        if (street.isEmpty()) return city;
        if (city.isEmpty()) return street;
        return street + ", " + city;
    }

    public String getFormattedPrice(FuelType fuel) {
        Double value = getPrecio(fuel);
        if (value == null || value <= 0) return "N/D";
        return String.format(Locale.getDefault(), "%.3f €", value);
    }

    public String getFormattedDistance() {
        if (distanceMeters == null || distanceMeters <= 0) return "";
        if (distanceMeters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m", distanceMeters);
        }
        return String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000.0);
    }

    public boolean hasPrice(FuelType fuel) {
        Double value = getPrecio(fuel);
        return value != null && value > 0;
    }
}