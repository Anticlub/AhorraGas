package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private static GasolineraRepository instance;

    private final GasolineraDataSource primary;

    // Cache en memoria (solo mientras la app está abierta)
    private List<Gasolinera> memoryCache;
    private DataSourceOrigin lastOrigin;

    public GasolineraRepository(GasolineraDataSource primary) {
        this.primary = primary;
    }

    public static synchronized GasolineraRepository getInstance(GasolineraDataSource primary) {
        if (instance == null) {
            instance = new GasolineraRepository(primary);
        }
        return instance;
    }

    public synchronized List<Gasolinera> getGasolineras() throws RepoError {

        // 1) Si ya está en memoria, devolver directamente
        if (memoryCache != null) {
            return memoryCache;
        }

        // 2) Cargar desde primary (CachedRemoteApiDataSource ya incluye fallback a cache de archivo)
        try {
            memoryCache = primary.loadGasolineras();

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
    }
}