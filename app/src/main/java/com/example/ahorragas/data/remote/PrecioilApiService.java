package com.example.ahorragas.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interfaz Retrofit para la API de Precioil.
 * Endpoint de histórico de precios por estación.
 */
public interface PrecioilApiService {

    /**
     * Obtiene el historial de precios de una estación en un rango de fechas.
     *
     * @param idEstacion  ID de la estación (coincide con IDEESS del Ministerio)
     * @param fechaInicio fecha de inicio en formato ISO 8601 (yyyy-MM-dd)
     * @param fechaFin    fecha de fin en formato ISO 8601 (yyyy-MM-dd)
     * @return Call con la respuesta JSON como String
     */
    @GET("estaciones/historico/{idEstacion}")
    Call<String> getHistorico(
            @Path("idEstacion") int idEstacion,
            @Query("fechaInicio") String fechaInicio,
            @Query("fechaFin") String fechaFin
    );
}