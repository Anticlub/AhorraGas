package com.example.ahorragas.util;

import com.example.ahorragas.model.FuelType;

public class UserPrefs {
    private FuelType combustible;
    private double consumo, radio, precioObjetivo;
    private boolean notifcacionDiaria, notificacionPrecio;

    //  CLASE BASE PARA DATOS DEL USUARIO
    public UserPrefs(FuelType combustible, double consumo, double radio,
                     double precioObjetivo, boolean notifcacionDiaria, boolean notificacionPrecio) {
        this.combustible = (combustible != null) ? combustible : FuelType.GASOLEO_A;
        this.consumo = consumo;
        this.radio = radio;
        this.precioObjetivo = precioObjetivo;
        this.notifcacionDiaria = notifcacionDiaria;
        this.notificacionPrecio = notificacionPrecio;
    }

    // ✅ Nuevo getter/setter tipado
    public FuelType getCombustible() {
        return combustible;
    }

    public void setCombustible(FuelType combustible) {
        this.combustible = (combustible != null) ? combustible : FuelType.GASOLEO_A;
    }

    // ✅ Compatibilidad por si en alguna parte usabas String
    public String getCombustibleAsString() {
        return combustible != null ? combustible.name() : FuelType.GASOLEO_A.name();
    }

    public void setCombustibleFromString(String value) {
        this.combustible = FuelType.fromString(value);
    }

    public double getConsumo() {
        return consumo;
    }

    public void setConsumo(double consumo) {
        this.consumo = consumo;
    }

    public double getRadio() {
        return radio;
    }

    public void setRadio(double radio) {
        this.radio = radio;
    }

    public double getPrecioObjetivo() {
        return precioObjetivo;
    }

    public void setPrecioObjetivo(double precioObjetivo) {
        this.precioObjetivo = precioObjetivo;
    }

    public boolean isNotifcacionDiaria() {
        return notifcacionDiaria;
    }

    public void setNotifcacionDiaria(boolean notifcacionDiaria) {
        this.notifcacionDiaria = notifcacionDiaria;
    }

    public boolean isNotificacionPrecio() {
        return notificacionPrecio;
    }

    public void setNotificacionPrecio(boolean notificacionPrecio) {
        this.notificacionPrecio = notificacionPrecio;
    }
}