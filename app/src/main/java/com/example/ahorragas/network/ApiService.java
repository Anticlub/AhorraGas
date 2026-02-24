package com.example.ahorragas.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    @GET("PreciosCarburantes/EstacionesTerrestres/FiltroMunicipioProducto/{idMunicipio}/{idProducto}")
    Call<GasStationsResponse> getStations(
            @Path("idMunicipio") int idMunicipio,
            @Path("idProducto") int idProducto
    );
}