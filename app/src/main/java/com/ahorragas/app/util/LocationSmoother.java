package com.ahorragas.app.util;

import java.util.ArrayDeque;

/**
 * Suavizado de ubicaciones mediante una media móvil de las últimas N muestras,
 * para evitar saltos bruscos del marcador de "mi ubicación". Trabaja solo con
 * lat/lon (double) para ser testable en JVM sin depender de android.location.
 */
public final class LocationSmoother {

    private static final int DEFAULT_SIZE = 5;

    private final int maxSize;
    private final ArrayDeque<double[]> buffer = new ArrayDeque<>();

    public LocationSmoother() {
        this(DEFAULT_SIZE);
    }

    public LocationSmoother(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
    }

    /**
     * Añade una muestra y devuelve la posición suavizada (media de las últimas
     * {@code maxSize} muestras).
     *
     * @param lat latitud de la muestra.
     * @param lon longitud de la muestra.
     * @return array {latSuavizada, lonSuavizada}.
     */
    public double[] add(double lat, double lon) {
        buffer.addLast(new double[]{lat, lon});
        if (buffer.size() > maxSize) {
            buffer.removeFirst();
        }
        double avgLat = 0, avgLon = 0;
        for (double[] point : buffer) {
            avgLat += point[0];
            avgLon += point[1];
        }
        avgLat /= buffer.size();
        avgLon /= buffer.size();
        return new double[]{avgLat, avgLon};
    }
}
