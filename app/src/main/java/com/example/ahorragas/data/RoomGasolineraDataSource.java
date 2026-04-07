package com.example.ahorragas.data;

import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.data.local.ConectorDao;
import com.example.ahorragas.data.local.EstacionDao;
import com.example.ahorragas.data.local.EstacionEntity;
import com.example.ahorragas.data.local.EstacionMapper;
import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link GasolineraDataSource} que lee desde Room.
 */
public class RoomGasolineraDataSource implements GasolineraDataSource {

    private final EstacionDao estacionDao;

    public RoomGasolineraDataSource(AppDatabase db) {
        this.estacionDao = db.estacionDao();
    }

    /**
     * Carga las gasolineras almacenadas en Room.
     *
     * @return lista de gasolineras o lista vacía si Room no tiene datos
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    @Override
    public List<Gasolinera> loadGasolineras() throws RepoError {
        try {
            List<EstacionEntity> entities = estacionDao.getAllGasolineras();
            List<Gasolinera> result = new ArrayList<>(entities.size());
            for (EstacionEntity e : entities) {
                result.add(EstacionMapper.toGasolinera(e));
            }
            return result;
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo gasolineras de Room: " + e.getMessage());
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
}