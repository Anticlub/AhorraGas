package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class CachedRemoteApiDataSource implements GasolineraDataSource {

    private static final long MAX_AGE_MS = 30L * 60L * 1000L;

    private DataSourceOrigin lastOrigin = null;
    private final RemoteApiDataSource remote;
    private final FileJsonCache cache;
    private final AssetsGasolineraDataSource assetsDataSource;
    private boolean forceRemoteNextLoad = false;
    public interface RefreshCallback {
        void onRefreshed(List<Gasolinera> updated);
        void onRefreshError(Exception error);
    }

    public CachedRemoteApiDataSource(Context context) {
        Context appContext = context.getApplicationContext();
        this.remote = new RemoteApiDataSource();
        this.cache = new FileJsonCache(appContext, "gasolineras_cache");
        this.assetsDataSource = new AssetsGasolineraDataSource(appContext);
    }

    public synchronized DataSourceOrigin getLastOrigin() {
        return lastOrigin;
    }

    public synchronized void requestForceRefresh() {
        forceRemoteNextLoad = true;
    }

    public synchronized void clearCache() {
        cache.clear();
        lastOrigin = null;
        forceRemoteNextLoad = false;
    }

    @Override
    public synchronized List<Gasolinera> loadGasolineras() throws Exception {
        boolean forceRemote = forceRemoteNextLoad;
        forceRemoteNextLoad = false;

        // 1) Si hay caché en disco → usarla inmediatamente
        if (!forceRemote && cache.hasCache()) {
            try {
                lastOrigin = DataSourceOrigin.CACHE;
                return GasolineraJsonParser.parse(cache.readJson());
            } catch (Exception ignored) {}
        }

        // 2) Sin caché → intentar assets precargados (primera instalación)
        try {
            List<Gasolinera> fromAssets = assetsDataSource.loadGasolineras();
            if (fromAssets != null && !fromAssets.isEmpty()) {
                lastOrigin = DataSourceOrigin.CACHE;
                return fromAssets;
            }
        } catch (Exception ignored) {}

        // 3) Sin assets → descargar de red
        String json = remote.downloadJson();
        cache.write(json);
        lastOrigin = DataSourceOrigin.REMOTE;
        return GasolineraJsonParser.parse(json);
    }
    /**
     * Descarga datos frescos de la red en background y notifica via callback.
     * Se llama después de haber servido la caché al usuario.
     *
     * @param callback notificado cuando lleguen datos frescos o haya error
     */
    public void refreshInBackground(RefreshCallback callback) {
        new Thread(() -> {
            try {
                String json = remote.downloadJson();
                cache.write(json);
                List<Gasolinera> updated = GasolineraJsonParser.parse(json);
                synchronized (CachedRemoteApiDataSource.this) {
                    lastOrigin = DataSourceOrigin.REMOTE;
                }
                callback.onRefreshed(updated);
            } catch (Exception e) {
                callback.onRefreshError(e);
            }
        }).start();
    }
}
