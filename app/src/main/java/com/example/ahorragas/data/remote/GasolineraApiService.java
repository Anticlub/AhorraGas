package com.example.ahorragas.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Interfaz Retrofit para el endpoint de gasolineras terrestres
 * del Ministerio de Industria.
 *
 * Devuelve el JSON como String porque el parseo lo realiza
 * {@link com.example.ahorragas.data.GasolineraJsonParser}.
 */
public interface GasolineraApiService {

    /**
     * Descarga el JSON completo de estaciones terrestres.
     *
     * @return Call con el cuerpo de la respuesta como String
     */
    @GET("EstacionesTerrestres/")
    Call<String> getEstacionesTerrestres();
}