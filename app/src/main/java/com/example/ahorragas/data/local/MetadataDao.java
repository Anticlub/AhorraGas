package com.example.ahorragas.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void set(MetadataEntity entry);

    @Query("SELECT valor FROM metadata WHERE clave = :clave")
    String get(String clave);

    @Query("SELECT valor FROM metadata WHERE clave = :clave")
    LiveData<String> observe(String clave);
}