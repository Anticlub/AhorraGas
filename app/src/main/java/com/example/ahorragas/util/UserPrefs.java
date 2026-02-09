package com.example.ahorragas.util;

public class UserPrefs {
    private String combustible;
    private double consumo, radio, precioObjetivo;
    private boolean notifcacionDiaria, notificacionPrecio;

    //  CLASE BASE PARA DATOS DEL USUARIO (AÑADIR O ELIMINAR SI ES NECESARIO)
    public UserPrefs(String combustible, double consumo, double radio, double precioObjetivo, boolean notifcacionDiaria, boolean notificacionPrecio) {
        this.combustible = combustible;
        this.consumo = consumo;
        this.radio = radio;
        this.precioObjetivo = precioObjetivo;
        this.notifcacionDiaria = notifcacionDiaria;
        this.notificacionPrecio = notificacionPrecio;
    }

    public String getCombustible() {
        return combustible;
    }

    public void setCombustible(String combustible) {
        this.combustible = combustible;
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
