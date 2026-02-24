package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RemoteApiDataSource implements GasolineraDataSource {

    private static final String ENDPOINT =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/";

    @Override
    public List<Gasolinera> loadGasolineras() throws Exception {

        String json = downloadJson();
        // De momento fuel fijo, luego lo hacemos dinámico
        return GasolineraJsonParser.parse(json, "Precio Gasoleo A");
    }

    protected String downloadJson() throws Exception {

        URL url = new URL(ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Ahorragas/1.0 (Android)");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        connection.disconnect();

        return result.toString();
    }
}