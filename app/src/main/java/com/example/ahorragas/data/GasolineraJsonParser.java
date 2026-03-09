package com.example.ahorragas.data;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.util.GeoValidation;
import com.example.ahorragas.util.NumberUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GasolineraJsonParser {

    private GasolineraJsonParser() {
    }

    public static List<Gasolinera> parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray array = root.optJSONArray("ListaEESSPrecio");
        if (array == null) return new ArrayList<>();

        List<Gasolinera> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);

            int id = safeInt(item.optString("IDEESS"));
            String marca = item.optString("Rótulo", "");
            String municipio = item.optString("Municipio", "");
            String direccion = item.optString("Dirección", "");
            String horario = item.optString("Horario", "");

            Double lat = NumberUtils.parseSpanishDouble(item.optString("Latitud", null));
            Double lon = NumberUtils.parseSpanishDouble(item.optString("Longitud (WGS84)", null));

            // Descartamos registros con coordenadas inválidas
            if (!GeoValidation.isValidLatLon(lat, lon)) {
                continue;
            }

            Gasolinera gasolinera = new Gasolinera(id, marca, municipio, direccion, lat, lon, null);
            gasolinera.setHorario(horario);

            for (FuelType fuel : FuelType.values()) {
                Double price = NumberUtils.parseSpanishDouble(item.optString(fuel.apiKey(), null));
                if (price != null && price > 0) {
                    gasolinera.setPrecio(fuel, price);
                } else {
                    gasolinera.setPrecio(fuel, null);
                }
            }

            result.add(gasolinera);
        }

        return result;
    }

    private static int safeInt(String value) {
        if (value == null) return 0;
        String clean = value.trim();
        if (clean.isEmpty()) return 0;
        try {
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 0;
        }
    }
}
