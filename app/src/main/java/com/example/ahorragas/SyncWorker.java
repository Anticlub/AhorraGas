package com.example.ahorragas;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ahorragas.data.ElectrolineraDataSource;
import com.example.ahorragas.data.RemoteDgtDataSource;
import com.example.ahorragas.data.RemoteApiDataSource;
import com.example.ahorragas.data.RoomElectrolineraDataSource;
import com.example.ahorragas.data.RoomGasolineraDataSource;
import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.data.local.MetadataEntity;
import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.model.Gasolinera;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Worker periódico que descarga gasolineras y electrolineras de la red
 * y las persiste en Room. Se ejecuta cada 2 horas con conexión de red.
 * Si falla, WorkManager lo reintentará en el siguiente ciclo.
 */
public class SyncWorker extends Worker {

    private static final String WORK_NAME          = "sync_estaciones";
    private static final long   INTERVAL_HOURS      = 2L;
    private static final String KEY_LAST_SYNC_GAS   = "last_sync_gasolineras";
    private static final String KEY_LAST_SYNC_ELEC  = "last_sync_electrolineras";
    private static final long   ELEC_MIN_INTERVAL_MS = TimeUnit.HOURS.toMillis(24);

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(ctx);
        RoomGasolineraDataSource roomGas = new RoomGasolineraDataSource(db);
        RoomElectrolineraDataSource roomElec = new RoomElectrolineraDataSource(db);

        boolean gasOk  = syncGasolineras(roomGas, db);
        boolean elecOk = syncElectrolineras(roomElec, db);

        return (gasOk && elecOk) ? Result.success() : Result.retry();
    }

    /**
     * Descarga gasolineras de la red y las persiste en Room.
     *
     * @param roomGas fuente Room de gasolineras
     * @param db      instancia de la base de datos
     * @return true si la sincronización fue exitosa
     */
    private boolean syncGasolineras(RoomGasolineraDataSource roomGas, AppDatabase db) {
        try {
            List<Gasolinera> gasolineras = new RemoteApiDataSource().loadGasolineras();
            roomGas.saveAll(gasolineras);
            saveTimestamp(db, KEY_LAST_SYNC_GAS);
            return true;
        } catch (Exception e) {
            android.util.Log.e("SyncWorker", "Error sincronizando gasolineras: " + e.getMessage());
            return false;
        }
    }

    /**
     * Descarga electrolineras de la red y las persiste en Room,
     * solo si han pasado más de 24 horas desde la última sincronización.
     *
     * @param roomElec fuente Room de electrolineras
     * @param db       instancia de la base de datos
     * @return true si la sincronización fue exitosa o no era necesaria
     */
    private boolean syncElectrolineras(RoomElectrolineraDataSource roomElec, AppDatabase db) {
        try {
            String lastStr = db.metadataDao().get(KEY_LAST_SYNC_ELEC);
            if (lastStr != null) {
                long lastSync = Long.parseLong(lastStr);
                if (System.currentTimeMillis() - lastSync < ELEC_MIN_INTERVAL_MS) {
                    return true; // no toca todavía
                }
            }
            List<Electrolinera> electrolineras = new RemoteDgtDataSource().loadElectrolineras();
            roomElec.saveAll(electrolineras);
            saveTimestamp(db, KEY_LAST_SYNC_ELEC);
            return true;
        } catch (Exception e) {
            android.util.Log.e("SyncWorker", "Error sincronizando electrolineras: " + e.getMessage());
            return false;
        }
    }

    /**
     * Guarda el timestamp actual en la tabla metadata.
     *
     * @param db    instancia de la base de datos
     * @param clave clave de metadata donde guardar el timestamp
     */
    private void saveTimestamp(AppDatabase db, String clave) {
        MetadataEntity entry = new MetadataEntity();
        entry.clave = clave;
        entry.valor = String.valueOf(System.currentTimeMillis());
        db.metadataDao().set(entry);
    }

    /**
     * Registra el trabajo periódico en WorkManager si no está ya registrado.
     * Usar {@link ExistingPeriodicWorkPolicy#KEEP} para no reiniciar el intervalo
     * si ya existe.
     *
     * @param context contexto de la aplicación
     */
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}