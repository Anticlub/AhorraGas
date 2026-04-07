package com.example.ahorragas.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.annotation.NonNull;

@Entity(tableName = "gasolineras")
public class EstacionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "station_id")
    public String stationId = "";

    public String marca;
    public String municipio;
    public String direccion;
    public String horario;
    public Double lat;
    public Double lon;

    @ColumnInfo(name = "es_electrica")
    public boolean esElectrica;

    public String operador;

    @ColumnInfo(name = "precio_gasolina95")       public Double precioGasolina95;
    @ColumnInfo(name = "precio_gasolina95_premium") public Double precioGasolina95Premium;
    @ColumnInfo(name = "precio_gasolina98")       public Double precioGasolina98;
    @ColumnInfo(name = "precio_gasoleo_a")        public Double precioGasoleoA;
    @ColumnInfo(name = "precio_gasoleo_premium")  public Double precioGasoleoPremium;
    @ColumnInfo(name = "precio_gasoleo_b")        public Double precioGasoleoB;
    @ColumnInfo(name = "precio_gasoleo_c")        public Double precioGasoleoC;
    @ColumnInfo(name = "precio_biodiesel")        public Double precioBiodiesel;
    @ColumnInfo(name = "precio_bioetanol")        public Double precioBioetanol;
    @ColumnInfo(name = "precio_glp")              public Double precioGlp;
    @ColumnInfo(name = "precio_gnc")              public Double precioGnc;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}