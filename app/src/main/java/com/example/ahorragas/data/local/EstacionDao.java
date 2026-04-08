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

    @Query("SELECT * FROM gasolineras WHERE es_electrica = 0 " +
            "AND lat BETWEEN :minLat AND :maxLat " +
            "AND lon BETWEEN :minLon AND :maxLon")
    List<EstacionEntity> getGasolinerasByBoundingBox(
            double minLat, double maxLat, double minLon, double maxLon);

    @Query("SELECT * FROM gasolineras WHERE es_electrica = 1 " +
            "AND lat BETWEEN :minLat AND :maxLat " +
            "AND lon BETWEEN :minLon AND :maxLon")
    List<EstacionEntity> getElectrolinerasByBoundingBox(
            double minLat, double maxLat, double minLon, double maxLon);

    @Query("SELECT * FROM gasolineras WHERE es_electrica = 0 " +
            "AND municipio = :municipio")
    List<EstacionEntity> getGasolinerasByMunicipio(String municipio);

    @Query("SELECT * FROM gasolineras WHERE es_electrica = 1 " +
            "AND municipio = :municipio")
    List<EstacionEntity> getElectrolinerasByMunicipio(String municipio);

    @Query("SELECT DISTINCT municipio FROM gasolineras " +
            "WHERE municipio LIKE :query || '%' " +
            "ORDER BY municipio ASC LIMIT 10")
    List<String> searchMunicipios(String query);

    @Query("DELETE FROM gasolineras WHERE es_electrica = 0")
    void deleteAllGasolineras();

    @Query("DELETE FROM gasolineras WHERE es_electrica = 1")
    void deleteAllElectrolineras();

    @Query("SELECT COUNT(*) FROM gasolineras WHERE es_electrica = 0")
    int countGasolineras();

    @Query("SELECT COUNT(*) FROM gasolineras WHERE es_electrica = 1")
    int countElectrolineras();
}