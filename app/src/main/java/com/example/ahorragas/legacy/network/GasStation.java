package com.example.ahorragas.legacy.network;

import com.google.gson.annotations.SerializedName;

public class GasStation {

    @SerializedName("Rótulo")
    public String rotulo;

    @SerializedName("Dirección")
    public String direccion;

    @SerializedName("Latitud")
    public String latitud;

    @SerializedName("Longitud (WGS84)")
    public String longitud;

    @SerializedName("PrecioProducto")
    public String precio;

    @SerializedName("Horario")
    public String horario;
}