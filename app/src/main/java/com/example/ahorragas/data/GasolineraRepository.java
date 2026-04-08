package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private static GasolineraRepository instance;
    private final RoomGasolineraDataSource roomDataSource;

    private GasolineraRepository(RoomGasolineraDataSource roomDataSource) {
        this.roomDataSource = roomDataSource;
    }

    public static synchronized GasolineraRepository getInstance(
            RoomGasolineraDataSource roomDataSource) {
        if (instance == null) {
            instance = new GasolineraRepository(roomDataSource);
        }
        return instance;
    }

    /**
     * Devuelve todas las gasolineras desde Room.
     *
     * @return lista de gasolineras
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getGasolineras() throws RepoError {
        return roomDataSource.loadGasolineras();
    }

    /**
     * Devuelve gasolineras dentro de un radio desde Room.
     *
     * @param lat          latitud del centro
     * @param lon          longitud del centro
     * @param radiusMeters radio en metros
     * @return lista de gasolineras en el radio
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getByRadius(double lat, double lon,
                                        double radiusMeters) throws RepoError {
        return roomDataSource.loadByRadius(lat, lon, radiusMeters);
    }

    /**
     * Devuelve gasolineras de un municipio concreto desde Room.
     *
     * @param municipio nombre exacto del municipio
     * @return lista de gasolineras del municipio
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getByMunicipio(String municipio) throws RepoError {
        return roomDataSource.loadByMunicipio(municipio);
    }

    /**
     * Invalida la caché en memoria forzando recarga en la siguiente llamada.
     */
    public void clearMemoryCache() {
        // No-op: Room es la fuente de verdad, no hay caché en memoria
    }
}