package com.example.ahorragas.data.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Interfaz Retrofit para el endpoint de electrolineras
 * de la DGT (formato XML).
 *
 * Devuelve {@link ResponseBody} en lugar de String porque
 * necesitamos acceder al InputStream directamente para
 * parsearlo con XmlPullParser.
 */
public interface ElectrolineraApiService {

    /**
     * Descarga el XML completo de electrolineras.
     *
     * @return Call con el cuerpo de la respuesta como ResponseBody
     */
    @GET("datex2/v3/miterd/EnergyInfrastructureTablePublication/electrolineras.xml")
    Call<ResponseBody> getElectrolineras();
}
