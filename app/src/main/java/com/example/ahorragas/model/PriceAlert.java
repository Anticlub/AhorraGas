package com.example.ahorragas.model;

/**
 * Representa una alerta de precio configurada por el usuario para una
 * gasolinera favorita y un tipo de combustible concreto.
 */
public class PriceAlert {

    private final int      gasolineraId;
    private final String   gasolineraName;
    private final FuelType fuelType;
    private final double   targetPrice;
    private long           lastNotifiedAt;

    /**
     * @param gasolineraId   ID único de la gasolinera.
     * @param gasolineraName Nombre visible de la gasolinera (marca + municipio).
     * @param fuelType       Tipo de combustible al que aplica la alerta.
     * @param targetPrice    Precio umbral: notifica cuando precio ≤ este valor.
     * @param lastNotifiedAt Timestamp en millis de la última notificación enviada.
     */
    public PriceAlert(int gasolineraId, String gasolineraName,
                      FuelType fuelType, double targetPrice, long lastNotifiedAt) {
        this.gasolineraId   = gasolineraId;
        this.gasolineraName = gasolineraName;
        this.fuelType       = fuelType;
        this.targetPrice    = targetPrice;
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public int getGasolineraId()      { return gasolineraId; }
    public String getGasolineraName() { return gasolineraName; }
    public FuelType getFuelType()     { return fuelType; }
    public double getTargetPrice()    { return targetPrice; }
    public long getLastNotifiedAt()   { return lastNotifiedAt; }

    public void setLastNotifiedAt(long lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    /**
     * Clave única que identifica esta alerta. Evita duplicados de la misma
     * gasolinera + combustible.
     *
     * @return String con formato "gasolineraId_fuelType"
     */
    public String getKey() {
        return gasolineraId + "_" + fuelType.name();
    }
}