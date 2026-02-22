package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private final GasolineraDataSource primary;
    private final GasolineraDataSource fallback;

    // Cache en memoria (solo mientras la app está abierta)
    private List<Gasolinera> memoryCache;
    private DataSourceOrigin lastOrigin;

    public GasolineraRepository(GasolineraDataSource primary, GasolineraDataSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public synchronized List<Gasolinera> getGasolineras() throws Exception {

        // 1) Si ya está en memoria, devolver directamente
        if (memoryCache != null) {
            return memoryCache;
        }

        // 2) Intentar primary (Remote con cache)
        try {
            memoryCache = primary.loadGasolineras();

            if (primary instanceof CachedRemoteApiDataSource) {
                lastOrigin = ((CachedRemoteApiDataSource) primary).getLastOrigin();
            } else {
                lastOrigin = DataSourceOrigin.REMOTE;
            }

        } catch (Exception e) {
            // 3) Fallback local si falla remote
            memoryCache = fallback.loadGasolineras();
            lastOrigin = DataSourceOrigin.LOCAL_FALLBACK;
        }

        return memoryCache;
    }

    public DataSourceOrigin getLastOrigin() {
        return lastOrigin;
    }

    // Por si en el futuro quieres forzar recarga
    public synchronized void clearMemoryCache() {
        memoryCache = null;
    }
}