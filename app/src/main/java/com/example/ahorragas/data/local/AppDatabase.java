package com.example.ahorragas.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {EstacionEntity.class, ConectorEntity.class, MetadataEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "ahorragas.db";
    private static volatile AppDatabase instance;

    public abstract EstacionDao estacionDao();
    public abstract ConectorDao conectorDao();
    public abstract MetadataDao metadataDao();

    /**
     * Devuelve la instancia única de la base de datos.
     * En la primera instalación, Room copia el fichero precargado de assets/.
     *
     * @param context contexto de la aplicación
     * @return instancia singleton de AppDatabase
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME)
                    .createFromAsset("ahorragas.db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}