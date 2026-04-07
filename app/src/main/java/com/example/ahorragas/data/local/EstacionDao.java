package com.example.ahorragas.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EstacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EstacionEntity> estaciones);

    @Query("SELECT * FROM gasolineras WHERE es_electrica = 0")
    List<EstacionEntity> getAllGasolineras();

    @Query("SELECT * FROM gasolineras WHERE es_electrica = 1")
    List<EstacionEntity> getAllElectrolineras();

    @Query("DELETE FROM gasolineras WHERE es_electrica = 0")
    void deleteAllGasolineras();

    @Query("DELETE FROM gasolineras WHERE es_electrica = 1")
    void deleteAllElectrolineras();

    @Query("SELECT COUNT(*) FROM gasolineras WHERE es_electrica = 0")
    int countGasolineras();

    @Query("SELECT COUNT(*) FROM gasolineras WHERE es_electrica = 1")
    int countElectrolineras();
}