package com.example.ahorragas.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modelo de dominio de una electrolinera obtenida de la API de la DGT.
 * Es un modelo interno que luego se mapea a {@link Gasolinera} en el repositorio unificado.
 */
public class Electrolinera {

    private String id;
    private String nombre;
    private String operador;
    private String direccion;
    private String municipio;
    private String provincia;
    private String horario;
    private Double lat;
    private Double lon;
    private List<Conector> conectores = new ArrayList<>();

    /**
     * Representa un punto de carga individual dentro de una electrolinera.
     */
    public static class Conector {
        private ConnectorType tipo;
        private String modoRecarga; // ej: "mode4DC"
        private Double potenciaW;   // en vatios — dividir entre 1000 para kW

        public Conector(ConnectorType tipo, String modoRecarga, Double potenciaW) {
            this.tipo        = tipo;
            this.modoRecarga = modoRecarga;
            this.potenciaW   = potenciaW;
        }

        public ConnectorType getTipo()     { return tipo; }
        public String getModoRecarga()     { return modoRecarga; }
        public Double getPotenciaW()       { return potenciaW; }

        /**
         * Devuelve la potencia en kW formateada.
         * El XML de la DGT la proporciona en vatios.
         *
         * @return potencia formateada (ej: "350 kW") o "N/D" si no está disponible
         */
        public String getFormattedPotencia() {
            if (potenciaW == null || potenciaW <= 0) return "N/D";
            double kw = potenciaW / 1000.0;
            return String.format(Locale.getDefault(), "%.0f kW", kw);
        }

        /**
         * Devuelve si la carga es rápida (DC) o normal (AC).
         *
         * @return "Rápida (DC)" o "Normal (AC)" según el modo de recarga
         */
        public String getTipoRecarga() {
            if (modoRecarga == null) return "";
            return modoRecarga.contains("DC") ? "Rápida (DC)" : "Normal (AC)";
        }

        /**
         * Resumen compacto del conector para mostrar en lista.
         * Ejemplo: "CCS2 · 350 kW"
         *
         * @return string resumen del conector
         */
        public String toResumen() {
            String nombre = tipo != null ? tipo.getDisplayName() : "Desconocido";
            return nombre + " · " + getFormattedPotencia();
        }
    }

    // =========
    // GETTERS / SETTERS
    // =========

    public String getId()                    { return id; }
    public void setId(String id)             { this.id = id; }

    public String getNombre()                { return nombre; }
    public void setNombre(String nombre)     { this.nombre = nombre; }

    public String getOperador()              { return operador; }
    public void setOperador(String operador) { this.operador = operador; }

    public String getDireccion()                 { return direccion; }
    public void setDireccion(String direccion)   { this.direccion = direccion; }

    public String getMunicipio()                 { return municipio; }
    public void setMunicipio(String municipio)   { this.municipio = municipio; }

    public String getProvincia()                 { return provincia; }
    public void setProvincia(String provincia)   { this.provincia = provincia; }

    public String getHorario()               { return horario; }
    public void setHorario(String horario)   { this.horario = horario; }

    public Double getLat()               { return lat; }
    public void setLat(Double lat)       { this.lat = lat; }

    public Double getLon()               { return lon; }
    public void setLon(Double lon)       { this.lon = lon; }

    public List<Conector> getConectores()            { return conectores; }
    public void setConectores(List<Conector> list)   { this.conectores = list; }

    public void addConector(Conector conector) {
        if (conector != null) conectores.add(conector);
    }

    /**
     * Devuelve un resumen de todos los conectores para mostrar en lista.
     * Ejemplo: "CCS2 · 350 kW  |  Tipo 2 · 22 kW"
     *
     * @return string con todos los conectores separados por " | "
     */
    public String getResumenConectores() {
        if (conectores == null || conectores.isEmpty()) return "Sin datos de conector";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conectores.size(); i++) {
            if (i > 0) sb.append("  |  ");
            sb.append(conectores.get(i).toResumen());
        }
        return sb.toString();
    }

    /**
     * Devuelve el conector de mayor potencia, útil para ordenar o destacar.
     *
     * @return Conector de mayor potencia o null si no hay conectores
     */
    public Conector getConectorMayorPotencia() {
        if (conectores == null || conectores.isEmpty()) return null;
        Conector mayor = conectores.get(0);
        for (Conector c : conectores) {
            if (c.getPotenciaW() != null &&
                    (mayor.getPotenciaW() == null || c.getPotenciaW() > mayor.getPotenciaW())) {
                mayor = c;
            }
        }
        return mayor;
    }
}