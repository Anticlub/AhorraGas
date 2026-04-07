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
}