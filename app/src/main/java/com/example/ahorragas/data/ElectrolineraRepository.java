package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;

import java.util.List;

public class ElectrolineraRepository {

    private static ElectrolineraRepository instance;

    private final ElectrolineraDataSource remoteDataSource;
    private final RoomElectrolineraDataSource roomDataSource;
    private List<Electrolinera> memoryCache;

    private ElectrolineraRepository(ElectrolineraDataSource remoteDataSource,
                                    RoomElectrolineraDataSource roomDataSource) {
        this.remoteDataSource = remoteDataSource;
        this.roomDataSource   = roomDataSource;
    }

    /**
     * Devuelve la instancia única del repositorio.
     *
     * @param remoteDataSource fuente remota de electrolineras
     * @param roomDataSource   fuente Room de electrolineras
     * @return instancia singleton
     */
    public static synchronized ElectrolineraRepository getInstance(
            ElectrolineraDataSource remoteDataSource,
            RoomElectrolineraDataSource roomDataSource) {
        if (instance == null) {
            instance = new ElectrolineraRepository(remoteDataSource, roomDataSource);
        }
        return instance;
    }

    /**
     * Devuelve electrolineras desde caché en memoria, Room, o red (en ese orden).
     *
     * @return lista de electrolineras
     * @throws RepoError si hay fallo en todas las fuentes
     */
    public synchronized List<Electrolinera> getElectrolineras() throws RepoError {

        // 1) Caché en memoria
        if (memoryCache != null) return memoryCache;

        // 2) Room como primera fuente persistente
        if (roomDataSource.hasData()) {
            try {
                List<Electrolinera> fromRoom = roomDataSource.loadElectrolineras();
                if (fromRoom != null && !fromRoom.isEmpty()) {
                    memoryCache = fromRoom;
                    return memoryCache;
                }
            } catch (Exception ignored) {}
        }

        // 3) Sin Room → descargar de red y persistir
        memoryCache = remoteDataSource.loadElectrolineras();

        if (memoryCache == null || memoryCache.isEmpty()) {
            throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                    "Sin datos de electrolineras");
        }

        final List<Electrolinera> toSave = memoryCache;
        new Thread(() -> {
            try { roomDataSource.saveAll(toSave); }
            catch (RepoError ignored) {}
        }).start();

        return memoryCache;
    }

    /**
     * Invalida la caché en memoria forzando recarga en la siguiente llamada.
     */
    public synchronized void clearMemoryCache() {
        memoryCache = null;
    }
}