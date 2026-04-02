package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private static GasolineraRepository instance;

    private GasolineraDataSource primary;

    // Cache en memoria con expiración de 30 minutos
    private static final long MEMORY_CACHE_MAX_AGE_MS = 30L * 60L * 1000L;
    private List<Gasolinera> memoryCache;
    private long memoryCacheTimestamp = 0L;
    private DataSourceOrigin lastOrigin;
    public GasolineraRepository(GasolineraDataSource primary) {
        this.primary = primary;
    }

    public static synchronized GasolineraRepository getInstance(GasolineraDataSource primary) {
        if (instance == null) {
            instance = new GasolineraRepository(primary);
        } else {
            instance.primary = primary;
        }
        return instance;
    }

    public synchronized List<Gasolinera> getGasolineras() throws RepoError {

        // 1) Si ya está en memoria y no ha expirado, devolver directamente
        long ageMs = System.currentTimeMillis() - memoryCacheTimestamp;
        if (memoryCache != null && ageMs <= MEMORY_CACHE_MAX_AGE_MS) {
            return memoryCache;
        }

        // Si ha expirado, limpiar para forzar recarga
        memoryCache = null;

        // 2) Cargar desde primary (CachedRemoteApiDataSource ya incluye fallback a cache de archivo)
        try {
            memoryCache = primary.loadGasolineras();
            memoryCacheTimestamp = System.currentTimeMillis();

            if (memoryCache == null || memoryCache.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE, "La fuente devolvió datos vacíos");
            }

            if (primary instanceof CachedRemoteApiDataSource) {
                lastOrigin = ((CachedRemoteApiDataSource) primary).getLastOrigin();
            } else {
                lastOrigin = DataSourceOrigin.REMOTE;
            }

            return memoryCache;

        } catch (RepoError e) {
            // Si el datasource ya lanza RepoError, lo propagamos tal cual
            throw e;

        } catch (Exception e) {
            // Convertimos cualquier excepción genérica a RepoError
            throw new RepoError(
                    RepoError.Type.NETWORK,
                    "Fallo cargando gasolineras: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
        }
    }

    public DataSourceOrigin getLastOrigin() {
        return lastOrigin;
    }

    public synchronized void clearMemoryCache() {
        memoryCache = null;
        memoryCacheTimestamp = 0L;
    }
}