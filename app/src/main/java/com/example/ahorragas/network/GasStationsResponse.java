package com.example.ahorragas.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GasStationsResponse {

    @SerializedName("ListaEESSPrecio")
    public List<GasStation> stations;
}