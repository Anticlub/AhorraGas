package com.example.ahorragas.data.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interfaz Retrofit para el endpoint de planes de descuento
 * del Geoportal de Gasolineras del Ministerio (formato CSV).
 *
 * Devuelve {@link ResponseBody} porque el CSV usa codificación
 * ISO-8859-1 y necesitamos controlar el charset al leerlo.
 */
public interface PromotionApiService {

    /**
     * Descarga el CSV de planes de descuento.
     *
     * @param cadena    filtro por cadena (vacío para todas)
     * @param extension formato de descarga
     * @return Call con el cuerpo de la respuesta
     */
    @GET("geoportal/downloadReportPlanes")
    Call<ResponseBody> getPromotions(
            @Query("cadena") String cadena,
            @Query("extension") String extension
    );
}