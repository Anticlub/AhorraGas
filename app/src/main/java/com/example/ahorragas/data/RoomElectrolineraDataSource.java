package com.example.ahorragas.data;

import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.data.local.ConectorDao;
import com.example.ahorragas.data.local.ConectorEntity;
import com.example.ahorragas.data.local.EstacionDao;
import com.example.ahorragas.data.local.EstacionEntity;
import com.example.ahorragas.data.local.EstacionMapper;
import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link ElectrolineraDataSource} que lee desde Room.
 */
public class RoomElectrolineraDataSource implements ElectrolineraDataSource {

    private final EstacionDao estacionDao;
    private final ConectorDao conectorDao;

    public RoomElectrolineraDataSource(AppDatabase db) {
        this.estacionDao = db.estacionDao();
        this.conectorDao = db.conectorDao();
    }

    /**
     * Carga las electrolineras almacenadas en Room.
     *
     * @return lista de electrolineras o lista vacía si Room no tiene datos
     * @throws RepoError si hay fallo de acceso a la base de datos
     */
    @Override
    public List<Electrolinera> loadElectrolineras() throws RepoError {
        try {
            List<EstacionEntity> entities = estacionDao.getAllElectrolineras();
            List<Electrolinera> result = new ArrayList<>(entities.size());
            for (EstacionEntity e : entities) {
                List<ConectorEntity> conectores = conectorDao.getByEstacion(e.stationId);
                result.add(EstacionMapper.toElectrolinera(e, conectores));
            }
            return result;
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error leyendo electrolineras de Room: " + e.getMessage());
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
}