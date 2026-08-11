package com.ahorragas.app.data.repository;

import com.ahorragas.app.data.remote.ApiClient;
import com.ahorragas.app.data.remote.NominatimApiService;
import com.ahorragas.app.data.remote.NominatimPlace;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Repositorio de geocodificación: convierte el nombre de una localidad en
 * coordenadas usando Nominatim (OpenStreetMap) vía Retrofit + Gson.
 *
 * Sustituye al antiguo HttpURLConnection + parseo manual del JSON, que era
 * frágil ante cambios de formato.
 */
public final class GeocodingRepository {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/";

    private static GeocodingRepository instance;

    private final NominatimApiService api;

    private GeocodingRepository() {
        api = ApiClient.getInstance().createGsonService(BASE_URL, NominatimApiService.class);
    }

    public static synchronized GeocodingRepository getInstance() {
        if (instance == null) {
            instance = new GeocodingRepository();
        }
        return instance;
    }

    /**
     * Geocodifica una ciudad de España. Llamada síncrona: ejecutar en segundo plano.
     *
     * @param city nombre de la localidad
     * @return {latitud, longitud}, o {@code null} si no se encuentra o la
     *         respuesta no es válida
     * @throws IOException si falla la red
     */
    public double[] geocodeCity(String city) throws IOException {
        Response<List<NominatimPlace>> response =
                api.search(city, "Spain", 1, "json").execute();

        if (!response.isSuccessful()) {
            return null;
        }
        List<NominatimPlace> body = response.body();
        if (body == null || body.isEmpty()) {
            return null;
        }
        NominatimPlace place = body.get(0);
        if (place.lat == null || place.lon == null) {
            return null;
        }
        try {
            return new double[]{Double.parseDouble(place.lat), Double.parseDouble(place.lon)};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
