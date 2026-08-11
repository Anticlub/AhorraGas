package com.ahorragas.app.data.remote;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de un resultado de Nominatim. Solo se mapean los campos que usa la app
 * (latitud y longitud, que llegan como strings en el JSON).
 *
 * {@code @Keep} evita que R8 elimine u ofusque los campos en el build de release.
 */
@Keep
public class NominatimPlace {

    @SerializedName("lat")
    public String lat;

    @SerializedName("lon")
    public String lon;
}
