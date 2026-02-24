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

    public synchronized List<Gasolinera> getGasolineras() throws RepoError {

        // 1) Si ya está en memoria, devolver directamente
        if (memoryCache != null) {
            return memoryCache;
        }

        // 2) Intentar primary (Remote con cache)
        try {
            memoryCache = primary.loadGasolineras();

            if (memoryCache == null || memoryCache.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE, "La fuente principal devolvió datos vacíos");
            }

            if (primary instanceof CachedRemoteApiDataSource) {
                lastOrigin = ((CachedRemoteApiDataSource) primary).getLastOrigin();
            } else {
                lastOrigin = DataSourceOrigin.REMOTE;
            }

            return memoryCache;

        } catch (RepoError e) {
            // Si la fuente primary ya “habla” RepoError, lo respetamos y probamos fallback igualmente.
            return tryFallbackOrThrow(e);

        } catch (Exception e) {
            // Si primary lanza Exception genérica, la convertimos a RepoError
            RepoError primaryError = new RepoError(
                    RepoError.Type.NETWORK,
                    "Fallo en fuente principal: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
            return tryFallbackOrThrow(primaryError);
        }
    }

    private List<Gasolinera> tryFallbackOrThrow(RepoError primaryError) throws RepoError {
        // 3) Fallback local si falla primary
        try {
            memoryCache = fallback.loadGasolineras();

            if (memoryCache == null || memoryCache.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE, "Fallback devolvió datos vacíos");
            }

            lastOrigin = DataSourceOrigin.LOCAL_FALLBACK;
            return memoryCache;

        } catch (RepoError fallbackError) {
            // Fallaron ambas con RepoError -> devolvemos el fallback (más relevante)
            throw fallbackError;

        } catch (Exception e) {
            // Falló el fallback con Exception genérica -> devolvemos un error claro
            throw new RepoError(
                    RepoError.Type.PARSE,
                    "Fallo también en fallback: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
        }
    }

    public DataSourceOrigin getLastOrigin() {
        return lastOrigin;
    }

    // Por si en el futuro quieres forzar recarga
    public synchronized void clearMemoryCache() {
        memoryCache = null;
    }
}