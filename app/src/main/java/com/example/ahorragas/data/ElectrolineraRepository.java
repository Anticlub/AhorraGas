package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;

import java.util.List;
import android.content.Context;

public class ElectrolineraRepository {

    private static ElectrolineraRepository instance;
    private final ElectrolineraDataSource dataSource;
    private List<Electrolinera> memoryCache;
    private final AssetsElectrolineraDataSource assetsDataSource;

    /**
     * Callback para notificar cuando lleguen electrolineras frescas de la red.
     */
    public interface RefreshCallback {
        void onRefreshed(List<Electrolinera> updated);
        void onRefreshError(RepoError error);
    }

    private ElectrolineraRepository(ElectrolineraDataSource dataSource, Context context) {
        this.dataSource = dataSource;
        this.assetsDataSource = new AssetsElectrolineraDataSource(context);
    }

    /**
     * Devuelve la instancia única del repositorio.
     *
     * @param dataSource fuente de datos a usar si aún no existe la instancia
     * @return instancia singleton de ElectrolineraRepository
     */
    public static synchronized ElectrolineraRepository getInstance(
            ElectrolineraDataSource dataSource, Context context) {
        if (instance == null) {
            instance = new ElectrolineraRepository(dataSource, context);
        }
        return instance;
    }

    /**
     * Devuelve la lista de electrolineras desde caché en memoria.
     * Si no hay caché, descarga y parsea (primera vez).
     *
     * @return lista de electrolineras
     * @throws RepoError si hay fallo de red, parseo o respuesta vacía
     */
    public synchronized List<Electrolinera> getElectrolineras() throws RepoError {
        if (memoryCache != null) return memoryCache;

        // 1) Intentar assets precargados (primera instalación)
        try {
            List<Electrolinera> fromAssets = assetsDataSource.loadElectrolineras();
            if (fromAssets != null && !fromAssets.isEmpty()) {
                memoryCache = fromAssets;
                return memoryCache;
            }
        } catch (Exception ignored) {}

        // 2) Sin assets → descargar de red
        memoryCache = dataSource.loadElectrolineras();

        if (memoryCache == null || memoryCache.isEmpty()) {
            throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                    "Sin datos de electrolineras");
        }

        return memoryCache;
    }

    /**
     * Descarga electrolineras frescas de la red en background y notifica via callback.
     * Se llama después de haber servido la caché al usuario.
     *
     * @param callback notificado cuando lleguen datos frescos o haya error
     */
    public void refreshInBackground(RefreshCallback callback) {
        new Thread(() -> {
            try {
                List<Electrolinera> updated = dataSource.loadElectrolineras();
                if (updated == null || updated.isEmpty()) {
                    callback.onRefreshError(new RepoError(
                            RepoError.Type.EMPTY_RESPONSE, "Sin datos de electrolineras"));
                    return;
                }
                synchronized (ElectrolineraRepository.this) {
                    memoryCache = updated;
                }
                callback.onRefreshed(updated);
            } catch (RepoError e) {
                callback.onRefreshError(e);
            }
        }).start();
    }

    public synchronized void clearMemoryCache() {
        memoryCache = null;
    }
}