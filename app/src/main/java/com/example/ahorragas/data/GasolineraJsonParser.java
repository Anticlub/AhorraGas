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

    private GasolineraJsonParser() {}

    public static List<Gasolinera> parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.optJSONArray("ListaEESSPrecio");
        if (arr == null) return new ArrayList<>();

        List<Gasolinera> result = new ArrayList<>(arr.length());

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            int id = safeInt(o.optString("IDEESS"));
            String marca = o.optString("Rótulo", "");
            String municipio = o.optString("Municipio", "");
            String direccion = o.optString("Dirección", "");

            Double lat = NumberUtils.parseSpanishDouble(o.optString("Latitud", null));
            Double lon = NumberUtils.parseSpanishDouble(o.optString("Longitud (WGS84)", null));

            if (!GeoValidation.isValidLatLon(lat, lon)) {
                continue; // descartamos registros con coords malas
            }

            Gasolinera g = new Gasolinera(id, marca, municipio, direccion, lat, lon, null);

            // ✅ Parsear TODOS los combustibles
            for (FuelType fuel : FuelType.values()) {
                Double precio = NumberUtils.parseSpanishDouble(o.optString(fuel.apiKey(), null));
                // Si viene 0, -1, etc., lo dejamos como null para no “ensuciar”
                if (precio != null && precio > 0) {
                    g.setPrecio(fuel, precio);
                } else {
                    g.setPrecio(fuel, null);
                }
            }

            result.add(g);
        }

        return result;
    }

    private static int safeInt(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}