package com.ahorragas.app.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.annotation.NonNull;

@Entity(tableName = "metadata")
public class MetadataEntity {

    @PrimaryKey
    @NonNull
    public String clave = "";

    public String valor;
}