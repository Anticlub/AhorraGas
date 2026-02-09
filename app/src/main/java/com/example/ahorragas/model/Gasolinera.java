package com.example.ahorragas.model;

public class Gasolinera {
    private int id;
    private String marca;
    private String direccion;
    private String municipio;
    private Double lat, lon, precio;

    // CLASE BASE GASOLINERA (COMPARAR CON JSON DE API PARA VER SI NECESITA O LE SOBRAN ATRIBUTOS)
    public Gasolinera() {
    }

    public Gasolinera(int id, String marca, String municipio, String direccion, Double lat, Double lon, Double precio) {
        this.id = id;
        this.marca = marca;
        this.municipio = municipio;
        this.direccion = direccion;
        this.lat = lat;
        this.lon = lon;
        this.precio = precio;
    }

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

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
}
