package com.example.ahorragas.model;

public class Station {

    private final String brand;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final Double price;     // puede ser null
    private final String schedule;  // puede ser null

    public Station(String brand, String address, double latitude, double longitude, Double price, String schedule) {
        this.brand = brand;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.price = price;
        this.schedule = schedule;
    }

    public String getBrand() { return brand; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Double getPrice() { return price; }
    public String getSchedule() { return schedule; }
}

