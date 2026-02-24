package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.model.Gasolinera;
//import com.example.ahorragas.model.VehiclePrefs;

import java.util.List;

public class CachedRemoteApiDataSource implements GasolineraDataSource {

    private static final long MAX_AGE_MS = 30L * 60L * 1000L; // 30 min
    private DataSourceOrigin lastOrigin = null;

    private final Context context;
    private final RemoteApiDataSource remote;
    private final FileJsonCache cache;
    public DataSourceOrigin getLastOrigin() {
        return lastOrigin;
    }

    public CachedRemoteApiDataSource(Context context) {
        this.context = context.getApplicationContext();
        this.remote = new RemoteApiDataSource();
        this.cache = new FileJsonCache(this.context, "gasolineras_cache");
    }

    @Override
    public List<Gasolinera> loadGasolineras() throws Exception {

        String fuelKey = "Precio Gasoleo A"; // de momento fijo

        // 1) Si existe cache y es reciente -> usar cache
        if (cache.hasCache()) {
            long ts = cache.readTimestamp();
            long age = System.currentTimeMillis() - ts;

            if (age <= MAX_AGE_MS) {
                String json = cache.readJson();
                lastOrigin = DataSourceOrigin.CACHE;
                return GasolineraJsonParser.parse(json, fuelKey);
            }
        }

        // 2) Si no hay cache válida -> descargar
        String json = remote.downloadJson();

        // Guardar en cache
        cache.write(json);

        lastOrigin = DataSourceOrigin.REMOTE;

        return GasolineraJsonParser.parse(json, fuelKey);
    }

    private String resolveFuelKey(String userFuel) {
        if (userFuel == null) return "Precio Gasoleo A";

        switch (userFuel) {
            case "Gasoleo A":
                return "Precio Gasoleo A";
            case "Gasolina 95":
                return "Precio Gasolina 95 E5";
            case "Gasolina 98":
                return "Precio Gasolina 98 E5";
            default:
                return "Precio Gasoleo A";
        }
    }
}