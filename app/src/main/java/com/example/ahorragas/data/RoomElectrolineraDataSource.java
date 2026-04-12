package com.example.ahorragas.data;

import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.data.local.ConectorDao;
import com.example.ahorragas.data.local.ConectorEntity;
import com.example.ahorragas.data.local.EstacionDao;
import com.example.ahorragas.data.local.EstacionEntity;
import com.example.ahorragas.data.local.EstacionMapper;
import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.util.GeoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link ElectrolineraDataSource} que lee y escribe desde Room.
 */
public class RoomElectrolineraDataSource implements ElectrolineraDataSource {

    private final EstacionDao estacionDao;
    private final ConectorDao conectorDao;

    public RoomElectrolineraDataSource(AppDatabase db) {
        this.estacionDao = db.estacionDao();
        this.conectorDao = db.conectorDao();
    }

    /**
     * Carga todas las electrolineras almacenadas en Room.
     *
     * @return lista de electrolineras o lista vacía si Room no tiene datos
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    @Override
    public List<Electrolinera> loadElectrolineras() throws RepoError {
        try {
            List<EstacionEntity> entities = estacionDao.getAllElectrolineras();
            return toElectrolineras(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo electrolineras de Room: " + e.getMessage());
        }
    }

    /**
     * Carga electrolineras dentro de un radio dado usando bounding box.
     *
     * @param lat          latitud del centro
     * @param lon          longitud del centro
     * @param radiusMeters radio en metros
     * @return lista de electrolineras dentro del radio
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    public List<Electrolinera> loadByRadius(double lat, double lon,
                                            double radiusMeters) throws RepoError {
        try {
            double[] bbox = GeoUtils.boundingBox(lat, lon, radiusMeters);
            List<EstacionEntity> entities = estacionDao.getElectrolinerasByBoundingBox(
                    bbox[0], bbox[1], bbox[2], bbox[3]);
            return toElectrolineras(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo electrolineras por radio: " + e.getMessage());
        }
    }

    /**
     * Carga electrolineras de un municipio concreto.
     *
     * @param municipio nombre exacto del municipio
     * @return lista de electrolineras del municipio
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    public List<Electrolinera> loadByMunicipio(String municipio) throws RepoError {
        try {
            List<EstacionEntity> entities =
                    estacionDao.getElectrolinerasByMunicipio(municipio);
            return toElectrolineras(entities);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo electrolineras por municipio: " + e.getMessage());
        }
    }

    /**
     * Reemplaza todas las electrolineras en Room con la lista proporcionada.
     *
     * @param electrolineras lista de electrolineras a persistir
     * @throws RepoError si hay fallo de escritura
     */
    public void saveAll(List<Electrolinera> electrolineras) throws RepoError {
        try {
            long now = System.currentTimeMillis();
            List<EstacionEntity> entities = new ArrayList<>(electrolineras.size());
            List<ConectorEntity> conectores = new ArrayList<>();
            for (Electrolinera el : electrolineras) {
                entities.add(EstacionMapper.fromElectrolinera(el, now));
                conectores.addAll(EstacionMapper.conectoresFromElectrolinera(el));
            }
            conectorDao.deleteAllDeElectrolineras();
            estacionDao.deleteAllElectrolineras();
            estacionDao.insertAll(entities);
            conectorDao.insertAll(conectores);
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error guardando electrolineras en Room: " + e.getMessage());
        }
    }

    /** @return true si Room tiene al menos una electrolinera */
    public boolean hasData() {
        return estacionDao.countElectrolineras() > 0;
    }

    /**
     * Convierte entidades de Room a objetos Electrolinera.
     * Carga todos los conectores en una sola query batch para
     * evitar el problema N+1.
     *
     * @param entities lista de entidades de estación
     * @return lista de electrolineras con sus conectores
     */
    private List<Electrolinera> toElectrolineras(List<EstacionEntity> entities) {
        if (entities.isEmpty()) return new ArrayList<>();

        // 1. Recoger todos los IDs de estación
        List<String> ids = new ArrayList<>(entities.size());
        for (EstacionEntity e : entities) {
            ids.add(e.stationId);
        }

        // 2. Cargar TODOS los conectores en una sola query
        List<ConectorEntity> allConectores = conectorDao.getByEstaciones(ids);

        // 3. Agrupar conectores por estacion_id en un mapa
        java.util.Map<String, List<ConectorEntity>> conectoresPorEstacion = new java.util.HashMap<>();
        for (ConectorEntity c : allConectores) {
            conectoresPorEstacion
                    .computeIfAbsent(c.estacionId, k -> new ArrayList<>())
                    .add(c);
        }

        // 4. Construir electrolineras asignando sus conectores desde el mapa
        List<Electrolinera> result = new ArrayList<>(entities.size());
        for (EstacionEntity e : entities) {
            List<ConectorEntity> conectores = conectoresPorEstacion.get(e.stationId);
            if (conectores == null) conectores = new ArrayList<>();
            result.add(EstacionMapper.toElectrolinera(e, conectores));
        }
        return result;
    }
}