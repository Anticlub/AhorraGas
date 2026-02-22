package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private final GasolineraDataSource primary;
    private final GasolineraDataSource fallback;

    // Cache en memoria (solo mientras la app está abierta)
    private List<Gasolinera> memoryCache;

    public GasolineraRepository(GasolineraDataSource primary, GasolineraDataSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public synchronized List<Gasolinera> getGasolineras() throws Exception {

        if (memoryCache != null) return memoryCache;

        try {
            memoryCache = primary.loadGasolineras();
        } catch (Exception e) {
            memoryCache = fallback.loadGasolineras();
        }

        return memoryCache;
    }

    // Por si en el futuro quieres forzar recarga
    public synchronized void clearMemoryCache() {
        memoryCache = null;
    }
}