package com.example.ahorragas.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "conectores",
        foreignKeys = @ForeignKey(
                entity = EstacionEntity.class,
                parentColumns = "station_id",
                childColumns = "estacion_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("estacion_id")
)
public class ConectorEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "estacion_id")
    public String estacionId;

    public String tipo;

    @ColumnInfo(name = "modo_recarga")
    public String modoRecarga;

    @ColumnInfo(name = "potencia_w")
    public Double potenciaW;
}