package com.example.ahorragas.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConectorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ConectorEntity> conectores);

    @Query("SELECT * FROM conectores WHERE estacion_id = :estacionId")
    List<ConectorEntity> getByEstacion(String estacionId);

    @Query("DELETE FROM conectores WHERE estacion_id IN (SELECT station_id FROM gasolineras WHERE es_electrica = 1)")
    void deleteAllDeElectrolineras();
    /**
     * Carga todos los conectores cuyo estacion_id esté en la lista dada.
     * Permite cargar conectores de múltiples estaciones en una sola query,
     * evitando el problema N+1.
     *
     * @param estacionIds lista de IDs de estaciones
     * @return todos los conectores de esas estaciones
     */
    @Query("SELECT * FROM conectores WHERE estacion_id IN (:estacionIds)")
    List<ConectorEntity> getByEstaciones(List<String> estacionIds);
}