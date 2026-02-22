package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.R;
import com.example.ahorragas.model.Gasolinera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocalJsonDataSource implements GasolineraDataSource {

    private final Context context;

    public LocalJsonDataSource(Context context) {
        this.context = context;
    }

    @Override
    public List<Gasolinera> loadGasolineras() throws Exception {

        String json = readRawJson();
        if (json == null || json.isEmpty()) return new ArrayList<>();

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
            Double precio = safeDouble(o.optString("PrecioProducto", null));

            Gasolinera g = new Gasolinera(id, marca, municipio, direccion, lat, lon, precio);
            result.add(g);
        }

        return result;
    }
    private Double safeDouble(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        // Convertir coma decimal a punto
        s = s.replace(",", ".");

        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private int safeInt(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;

        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private String readRawJson() throws Exception {
        InputStream is = context.getResources().openRawResource(R.raw.gasolineras_spain);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
}