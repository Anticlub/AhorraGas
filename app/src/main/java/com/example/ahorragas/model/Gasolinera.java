package com.example.ahorragas.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Gasolinera implements android.os.Parcelable {
    private int id;
    private String marca;
    private String direccion;
    private String municipio;
    private String horario;
    private Double lat;
    private Double lon;
    private Double distanceMeters;
    private PriceLevel priceLevel = PriceLevel.UNKNOWN;
    // Campos exclusivos de electrolineras (null en gasolineras normales)
    private boolean electric = false;
    private String operador;
    private List<Electrolinera.Conector> conectores;

    private final Map<FuelType, Double> precios = new EnumMap<>(FuelType.class);

    public Gasolinera() {
    }

    public Gasolinera(int id, String marca, String municipio, String direccion,
                      Double lat, Double lon, Double precio) {
        this.id = id;
        this.marca = marca;
        this.municipio = municipio;
        this.direccion = direccion;
        this.lat = lat;
        this.lon = lon;
        setPrecio(FuelType.GASOLEO_A, precio);

    }

    // ======================
    // GETTERS / SETTERS BASE
    // ======================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario == null ? null : horario.trim();
    }

    public String getFormattedHorario() {
        if (horario == null || horario.trim().isEmpty()) return null;
        return horario.trim();
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public PriceLevel getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel != null ? priceLevel : PriceLevel.UNKNOWN;
    }

    public void setPrecio(FuelType fuel, Double value) {
        if (fuel == null) return;
        precios.put(fuel, value);
    }

    public Double getPrecio(FuelType fuel) {
        if (fuel == null) return null;
        return precios.get(fuel);
    }

    public Double getPrecio() {
        return getPrecio(FuelType.GASOLEO_A);
    }

    public void setPrecio(Double value) {
        setPrecio(FuelType.GASOLEO_A, value);
    }

    public String getBrandInitial() {
        String raw = marca != null ? marca.trim() : "";
        if (raw.isEmpty()) return "?";

        String cleaned = raw.replaceAll("[^\\p{L}\\p{N} ]", "").trim();
        if (cleaned.isEmpty()) cleaned = raw;

        String[] parts = cleaned.split("\\s+");
        if (parts.length >= 2) {
            String abbr = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.getDefault());
            return abbr.length() > 2 ? abbr.substring(0, 2) : abbr;
        }

        return cleaned.substring(0, Math.min(2, cleaned.length())).toUpperCase(Locale.getDefault());
    }

    public String getDisplayAddress() {
        String street = direccion != null ? direccion.trim() : "";
        String city = municipio != null ? municipio.trim() : "";

        if (street.isEmpty() && city.isEmpty()) return "Dirección no disponible";
        if (street.isEmpty()) return city;
        if (city.isEmpty()) return street;
        return street + ", " + city;
    }

    public String getFormattedPrice(FuelType fuel) {
        Double value = getPrecio(fuel);
        if (value == null || value <= 0) return "N/D";
        return String.format(Locale.getDefault(), "%.3f €", value);
    }

    public String getFormattedDistance() {
        if (distanceMeters == null || distanceMeters <= 0) return "";
        if (distanceMeters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m", distanceMeters);
        }
        return String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000.0);
    }

    public boolean hasPrice(FuelType fuel) {
        Double value = getPrecio(fuel);
        return value != null && value > 0;
    }

    protected Gasolinera(android.os.Parcel in) {
        id = in.readInt();
        marca = in.readString();
        direccion = in.readString();
        municipio = in.readString();
        horario = in.readString();
        lat = in.readByte() == 0 ? null : in.readDouble();
        lon = in.readByte() == 0 ? null : in.readDouble();
        distanceMeters = in.readByte() == 0 ? null : in.readDouble();
        priceLevel = PriceLevel.valueOf(in.readString());
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            FuelType fuel = FuelType.valueOf(in.readString());
            Double price = in.readByte() == 0 ? null : in.readDouble();
            precios.put(fuel, price);
        }
        electric = in.readByte() != 0;
        operador = in.readString();
        String conectoresRaw = in.readString();
        if (conectoresRaw != null && !conectoresRaw.isEmpty()) {
            conectores = new java.util.ArrayList<>();
            for (String item : conectoresRaw.split(";")) {
                String[] parts = item.split("\\|", -1);
                if (parts.length < 3) continue;
                ConnectorType tipo;
                try { tipo = ConnectorType.valueOf(parts[0]); }
                catch (Exception e) { tipo = ConnectorType.UNKNOWN; }
                String modo = parts[1];
                Double potencia = null;
                try { potencia = Double.parseDouble(parts[2]); }
                catch (Exception ignored) {}
                conectores.add(new Electrolinera.Conector(tipo, modo, potencia));
            }
        }
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(marca);
        dest.writeString(direccion);
        dest.writeString(municipio);
        dest.writeString(horario);
        if (lat == null) { dest.writeByte((byte) 0); } else { dest.writeByte((byte) 1); dest.writeDouble(lat); }
        if (lon == null) { dest.writeByte((byte) 0); } else { dest.writeByte((byte) 1); dest.writeDouble(lon); }
        if (distanceMeters == null) { dest.writeByte((byte) 0); } else { dest.writeByte((byte) 1); dest.writeDouble(distanceMeters); }
        dest.writeString(priceLevel.name());
        dest.writeInt(precios.size());
        for (Map.Entry<FuelType, Double> entry : precios.entrySet()) {
            Double price = entry.getValue();
            dest.writeString(entry.getKey().name());
            if (price == null) {
                dest.writeByte((byte) 0);
            } else {
                dest.writeByte((byte) 1);
                dest.writeDouble(price);
            }
        }
        dest.writeByte((byte) (electric ? 1 : 0));
        dest.writeString(operador != null ? operador : "");
// Serializar conectores como string: "TIPO|modo|potencia;TIPO|modo|potencia"
        if (conectores == null || conectores.isEmpty()) {
            dest.writeString("");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < conectores.size(); i++) {
                Electrolinera.Conector c = conectores.get(i);
                if (i > 0) sb.append(";");
                sb.append(c.getTipo() != null ? c.getTipo().name() : "UNKNOWN");
                sb.append("|");
                sb.append(c.getModoRecarga() != null ? c.getModoRecarga() : "");
                sb.append("|");
                sb.append(c.getPotenciaW() != null ? c.getPotenciaW() : "0");
            }
            dest.writeString(sb.toString());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final android.os.Parcelable.Creator<Gasolinera> CREATOR =
            new android.os.Parcelable.Creator<Gasolinera>() {
                @Override
                public Gasolinera createFromParcel(android.os.Parcel in) {
                    return new Gasolinera(in);
                }

                @Override
                public Gasolinera[] newArray(int size) {
                    return new Gasolinera[size];
                }
            };

    public boolean isElectric()                              { return electric; }
    public void setElectric(boolean electric)                { this.electric = electric; }

    public String getOperador()                              { return operador; }
    public void setOperador(String operador)                 { this.operador = operador; }

    public List<Electrolinera.Conector> getConectores()      { return conectores; }
    public void setConectores(List<Electrolinera.Conector> conectores) { this.conectores = conectores; }

    /**
     * Devuelve el resumen de conectores si es una electrolinera.
     *
     * @return resumen de conectores o null si no es eléctrica
     */
    public String getResumenConectores() {
        if (!electric || conectores == null || conectores.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conectores.size(); i++) {
            if (i > 0) sb.append("  |  ");
            sb.append(conectores.get(i).toResumen());
        }
        return sb.toString();
    }

    /**
     * Devuelve el resumen del conector de mayor potencia para mostrar en lista.
     * Ejemplo: "CCS2 · 350 kW"
     *
     * @return string con el conector de mayor potencia o null si no es eléctrica
     */
    public String getResumenMejorConector() {
        if (!electric || conectores == null || conectores.isEmpty()) return null;
        Electrolinera.Conector mejor = null;
        for (Electrolinera.Conector c : conectores) {
            if (mejor == null || (c.getPotenciaW() != null &&
                    (mejor.getPotenciaW() == null || c.getPotenciaW() > mejor.getPotenciaW()))) {
                mejor = c;
            }
        }
        return mejor != null ? mejor.toResumen() : null;
    }
}