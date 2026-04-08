package com.example.ahorragas.data;

import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.data.local.EstacionDao;
import com.example.ahorragas.data.local.EstacionEntity;
import com.example.ahorragas.data.local.EstacionMapper;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.util.GeoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link GasolineraDataSource} que lee y escribe desde Room.
 */
public class RoomGasolineraDataSource implements GasolineraDataSource {

    private final EstacionDao estacionDao;

    public RoomGasolineraDataSource(AppDatabase db) {
        this.estacionDao = db.estacionDao();
    }

    /**
     * Carga todas las gasolineras almacenadas en Room.
     *
     * @return lista de gasolineras o lista vacía si Room no tiene datos
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    @Override
    public List<Gasolinera> loadGasolineras() throws RepoError {
        try {
            List<EstacionEntity> entities = estacionDao.getAllGasolineras();
            return toGasolineras(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo gasolineras de Room: " + e.getMessage());
        }
    }

    /**
     * Carga gasolineras dentro de un radio dado usando bounding box.
     *
     * @param lat          latitud del centro
     * @param lon          longitud del centro
     * @param radiusMeters radio en metros
     * @return lista de gasolineras dentro del radio
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    public List<Gasolinera> loadByRadius(double lat, double lon,
                                         double radiusMeters) throws RepoError {
        try {
            double[] bbox = GeoUtils.boundingBox(lat, lon, radiusMeters);
            List<EstacionEntity> entities = estacionDao.getGasolinerasByBoundingBox(
                    bbox[0], bbox[1], bbox[2], bbox[3]);
            return toGasolineras(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo gasolineras por radio: " + e.getMessage());
        }
    }

    /**
     * Carga gasolineras de un municipio concreto.
     *
     * @param municipio nombre exacto del municipio
     * @return lista de gasolineras del municipio
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    public List<Gasolinera> loadByMunicipio(String municipio) throws RepoError {
        try {
            List<EstacionEntity> entities =
                    estacionDao.getGasolinerasByMunicipio(municipio);
            return toGasolineras(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo gasolineras por municipio: " + e.getMessage());
        }
    }

    /**
     * Reemplaza todas las gasolineras en Room con la lista proporcionada.
     *
     * @param gasolineras lista de gasolineras a persistir
     * @throws RepoError si hay fallo de escritura
     */
    public void saveAll(List<Gasolinera> gasolineras) throws RepoError {
        try {
            long now = System.currentTimeMillis();
            List<EstacionEntity> entities = new ArrayList<>(gasolineras.size());
            for (Gasolinera g : gasolineras) {
                entities.add(EstacionMapper.fromGasolinera(g, now));
            }
            estacionDao.deleteAllGasolineras();
            estacionDao.insertAll(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error guardando gasolineras en Room: " + e.getMessage());
        }
    }

    /** @return true si Room tiene al menos una gasolinera */
    public boolean hasData() {
        return estacionDao.countGasolineras() > 0;
    }

    private List<Gasolinera> toGasolineras(List<EstacionEntity> entities) {
        List<Gasolinera> result = new ArrayList<>(entities.size());
        for (EstacionEntity e : entities) {
            result.add(EstacionMapper.toGasolinera(e));
        }
        return result;
    }
}