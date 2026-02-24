package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.util.GeoValidation;
import com.example.ahorragas.util.NumberUtils;

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

            Double lat = NumberUtils.parseSpanishDouble(o.optString("Latitud", null));
            Double lon = NumberUtils.parseSpanishDouble(o.optString("Longitud (WGS84)", null));

            if (!GeoValidation.isValidLatLon(lat, lon)) {
                continue; // descartamos registros con coords malas (0,0, fuera de rango, etc.)
            }

            Double precio = NumberUtils.parseSpanishDouble(o.optString(fuelKey, null));

            result.add(new Gasolinera(id, marca, municipio, direccion, lat, lon, precio));;
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