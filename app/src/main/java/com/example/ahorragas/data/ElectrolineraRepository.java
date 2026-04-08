package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;

import java.util.List;

public class ElectrolineraRepository {

    private static ElectrolineraRepository instance;
    private final RoomElectrolineraDataSource roomDataSource;
    private final ElectrolineraDataSource remoteDataSource;

    private ElectrolineraRepository(ElectrolineraDataSource remoteDataSource,
                                    RoomElectrolineraDataSource roomDataSource) {
        this.remoteDataSource = remoteDataSource;
        this.roomDataSource   = roomDataSource;
    }

    public static synchronized ElectrolineraRepository getInstance(
            ElectrolineraDataSource remoteDataSource,
            RoomElectrolineraDataSource roomDataSource) {
        if (instance == null) {
            instance = new ElectrolineraRepository(remoteDataSource, roomDataSource);
        }
        return instance;
    }

    /**
     * Devuelve todas las electrolineras desde Room.
     * Si Room no tiene datos, descarga de red y persiste.
     *
     * @return lista de electrolineras
     * @throws RepoError si hay fallo
     */
    public List<Electrolinera> getElectrolineras() throws RepoError {
        if (roomDataSource.hasData()) {
            return roomDataSource.loadElectrolineras();
        }
        List<Electrolinera> fromRemote = remoteDataSource.loadElectrolineras();
        if (fromRemote == null || fromRemote.isEmpty()) {
            throw new RepoError(RepoError.Type.EMPTY_RESPONSE, "Sin datos de electrolineras");
        }
        new Thread(() -> {
            try { roomDataSource.saveAll(fromRemote); }
            catch (RepoError ignored) {}
        }).start();
        return fromRemote;
    }

    /**
     * Devuelve electrolineras dentro de un radio desde Room.
     *
     * @param lat          latitud del centro
     * @param lon          longitud del centro
     * @param radiusMeters radio en metros
     * @return lista de electrolineras en el radio
     * @throws RepoError si hay fallo
     */
    public List<Electrolinera> getByRadius(double lat, double lon,
                                           double radiusMeters) throws RepoError {
        return roomDataSource.loadByRadius(lat, lon, radiusMeters);
    }

    /**
     * Devuelve electrolineras de un municipio concreto desde Room.
     *
     * @param municipio nombre exacto del municipio
     * @return lista de electrolineras del municipio
     * @throws RepoError si hay fallo
     */
    public List<Electrolinera> getByMunicipio(String municipio) throws RepoError {
        return roomDataSource.loadByMunicipio(municipio);
    }

    /**
     * Invalida la caché en memoria.
     */
    public void clearMemoryCache() {
        // No-op: Room es la fuente de verdad
    }
}