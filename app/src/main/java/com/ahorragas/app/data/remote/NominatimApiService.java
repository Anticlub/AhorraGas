package com.ahorragas.app.data.remote;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Servicio Retrofit para el geocodificador Nominatim de OpenStreetMap.
 * Convierte el nombre de una localidad en coordenadas.
 *
 * Política de uso: https://operations.osmfoundation.org/policies/nominatim/
 * (requiere User-Agent válido — lo pone el OkHttpClient de ApiClient — y
 * un máximo de 1 petición por segundo).
 */
public interface NominatimApiService {

    @GET("search")
    Call<List<NominatimPlace>> search(
            @Query("city") String city,
            @Query("country") String country,
            @Query("limit") int limit,
            @Query("format") String format);
}
