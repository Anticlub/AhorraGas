package com.example.ahorragas.network;

import com.example.ahorragas.model.Station;

public class StationMapper {

    public static Station fromDto(GasStation dto) {
        if (dto == null) return null;

        Double lat = NumberUtils.parseSpanishDouble(dto.latitud);
        Double lon = NumberUtils.parseSpanishDouble(dto.longitud);

        if (lat == null || lon == null) return null;
        if (!isValidLatLon(lat, lon)) return null;

        Double price = NumberUtils.parseSpanishDouble(dto.precio);

        String brand = safeTrim(dto.rotulo);
        String address = safeTrim(dto.direccion);
        String schedule = safeTrim(dto.horario);

        return new Station(brand, address, lat, lon, price, schedule);
    }

    private static boolean isValidLatLon(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}