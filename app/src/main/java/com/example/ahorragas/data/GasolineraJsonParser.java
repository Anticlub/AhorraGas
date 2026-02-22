package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GasolineraJsonParser {

    private GasolineraJsonParser() {}

    public static List<Gasolinera> parse(String json, String fuelKey) throws Exception {
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

            Double lat = safeDouble(o.optString("Latitud", null));
            Double lon = safeDouble(o.optString("Longitud (WGS84)", null));

            Double precio = safeDouble(o.optString(fuelKey, null)); // <- clave dinámica

            result.add(new Gasolinera(id, marca, municipio, direccion, lat, lon, precio));
        }

        return result;
    }

    private static Double safeDouble(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        s = s.replace(",", ".");
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private static int safeInt(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}