package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class CachedRemoteApiDataSource implements GasolineraDataSource {

    private static final long MAX_AGE_MS = 30L * 60L * 1000L;

    private DataSourceOrigin lastOrigin = null;
    private final RemoteApiDataSource remote;
    private final FileJsonCache cache;
    private boolean forceRemoteNextLoad = false;

    public CachedRemoteApiDataSource(Context context) {
        Context appContext = context.getApplicationContext();
        this.remote = new RemoteApiDataSource();
        this.cache = new FileJsonCache(appContext, "gasolineras_cache");
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
        
        // Si existe caché válida, usar caché
        if (!forceRemote && cache.hasCache()) {
            try {
                long age = System.currentTimeMillis() - cache.readTimestamp();
                if (age <= MAX_AGE_MS) {
                    lastOrigin = DataSourceOrigin.CACHE;
                    return GasolineraJsonParser.parse(cache.readJson());
                }
            } catch (Exception ignored) {

            }
        }

        try {
            String json = remote.downloadJson();
            cache.write(json);
            lastOrigin = DataSourceOrigin.REMOTE;
            return GasolineraJsonParser.parse(json);
        } catch (Exception remoteError) {
            if (cache.hasCache()) {
                try {
                    lastOrigin = DataSourceOrigin.CACHE;
                    return GasolineraJsonParser.parse(cache.readJson());
                } catch (Exception ignored) {

                }
            }
            throw remoteError;
        }
    }
}
