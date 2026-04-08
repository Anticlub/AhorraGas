package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private static GasolineraRepository instance;

    private GasolineraDataSource primary;
    private final RoomGasolineraDataSource roomDataSource;

    private static final long MEMORY_CACHE_MAX_AGE_MS = 30L * 60L * 1000L;
    private List<Gasolinera> memoryCache;
    private long memoryCacheTimestamp = 0L;
    private DataSourceOrigin lastOrigin;

    private GasolineraRepository(GasolineraDataSource primary,
                                 RoomGasolineraDataSource roomDataSource) {
        this.primary        = primary;
        this.roomDataSource = roomDataSource;
    }

    public static synchronized GasolineraRepository getInstance(
            GasolineraDataSource primary,
            RoomGasolineraDataSource roomDataSource) {
        if (instance == null) {
            instance = new GasolineraRepository(primary, roomDataSource);
        } else {
            instance.primary = primary;
        }
        return instance;
    }

    /**
     * Devuelve gasolineras desde caché en memoria, Room, o red (en ese orden).
     *
     * @return lista de gasolineras
     * @throws RepoError si hay fallo en todas las fuentes
     */
    public synchronized List<Gasolinera> getGasolineras() throws RepoError {

        // 1) Caché en memoria válida
        long ageMs = System.currentTimeMillis() - memoryCacheTimestamp;
        if (memoryCache != null && ageMs <= MEMORY_CACHE_MAX_AGE_MS) {
            return memoryCache;
        }
        memoryCache = null;

        // 2) Room como primera fuente persistente
        if (roomDataSource.hasData()) {
            try {
                List<Gasolinera> fromRoom = roomDataSource.loadGasolineras();
                if (fromRoom != null && !fromRoom.isEmpty()) {
                    memoryCache = fromRoom;
                    memoryCacheTimestamp = System.currentTimeMillis();
                    lastOrigin = DataSourceOrigin.CACHE;
                    return memoryCache;
                }
            } catch (Exception ignored) {}
        }

        // 3) Sin Room → cargar desde red y persistir
        try {
            memoryCache = primary.loadGasolineras();
            memoryCacheTimestamp = System.currentTimeMillis();

            if (memoryCache == null || memoryCache.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                        "La fuente devolvió datos vacíos");
            }

            if (primary instanceof CachedRemoteApiDataSource) {
                lastOrigin = ((CachedRemoteApiDataSource) primary).getLastOrigin();
            } else {
                lastOrigin = DataSourceOrigin.REMOTE;
            }

            final List<Gasolinera> toSave = memoryCache;
            new Thread(() -> {
                try { roomDataSource.saveAll(toSave); }
                catch (RepoError ignored) {}
            }).start();

            return memoryCache;

        } catch (RepoError e) {
            throw e;
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Fallo cargando gasolineras: " + (e.getMessage() != null
                            ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    public DataSourceOrigin getLastOrigin() {
        return lastOrigin;
    }

    /**
     * Invalida la caché en memoria forzando recarga en la siguiente llamada.
     */
    public synchronized void clearMemoryCache() {
        memoryCache = null;
        memoryCacheTimestamp = 0L;
    }
}