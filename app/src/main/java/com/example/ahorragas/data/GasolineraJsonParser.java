package com.example.ahorragas.data;

import android.util.JsonReader;
import android.util.JsonToken;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.util.GeoValidation;
import com.example.ahorragas.util.NumberUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class GasolineraJsonParser {

    private GasolineraJsonParser() {
    }

    /**
     * Parsea el JSON de gasolineras del Ministerio en streaming,
     * sin cargar el documento completo en memoria.
     *
     * @param reader Reader sobre el stream de la respuesta HTTP
     * @return lista de gasolineras válidas parseadas
     * @throws Exception si el JSON tiene un formato inesperado
     */
    public static List<Gasolinera> parse(Reader reader) throws Exception {
        List<Gasolinera> result = new ArrayList<>();

        try (JsonReader jr = new JsonReader(reader)) {
            jr.beginObject();
            while (jr.hasNext()) {
                String rootKey = jr.nextName();
                if ("ListaEESSPrecio".equals(rootKey)) {
                    jr.beginArray();
                    while (jr.hasNext()) {
                        Gasolinera g = parseEstacion(jr);
                        if (g != null) result.add(g);
                    }
                    jr.endArray();
                } else {
                    jr.skipValue();
                }
            }
            jr.endObject();
        }

        return result;
    }

    /**
     * Parsea un único objeto de estación del array ListaEESSPrecio.
     * Devuelve null si las coordenadas son inválidas.
     *
     * @param jr JsonReader posicionado al inicio del objeto
     * @return Gasolinera parseada o null si debe descartarse
     */
    private static Gasolinera parseEstacion(JsonReader jr) throws Exception {
        int id = 0;
        String marca = "", municipio = "", direccion = "", horario = "";
        Double lat = null, lon = null;
        java.util.Map<FuelType, Double> precios = new java.util.EnumMap<>(FuelType.class);

        jr.beginObject();
        while (jr.hasNext()) {
            if (jr.peek() == JsonToken.NULL) { jr.skipValue(); continue; }
            String key = jr.nextName();
            if (jr.peek() == JsonToken.NULL) { jr.skipValue(); continue; }
            String value = jr.nextString();

            switch (key) {
                case "IDEESS":     id        = safeInt(value);  break;
                case "Rótulo":     marca     = value;           break;
                case "Municipio":  municipio = value;           break;
                case "Dirección":  direccion = value;           break;
                case "Horario":    horario   = value.trim();    break;
                case "Latitud":    lat = NumberUtils.parseSpanishDouble(value); break;
                case "Longitud (WGS84)": lon = NumberUtils.parseSpanishDouble(value); break;
                default:
                    for (FuelType fuel : FuelType.values()) {
                        if (key.equals(fuel.apiKey())) {
                            Double price = NumberUtils.parseSpanishDouble(value);
                            if (price != null && price > 0) precios.put(fuel, price);
                            break;
                        }
                    }
            }
        }
        jr.endObject();

        if (!GeoValidation.isValidLatLon(lat, lon)) return null;

        Gasolinera g = new Gasolinera(id, marca, municipio, direccion, lat, lon, null);
        g.setHorario(horario);
        for (FuelType fuel : FuelType.values()) {
            g.setPrecio(fuel, precios.get(fuel));
        }
        return g;
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