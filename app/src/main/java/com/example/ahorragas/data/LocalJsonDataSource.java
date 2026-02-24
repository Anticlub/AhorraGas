package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.R;
import com.example.ahorragas.model.Gasolinera;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocalJsonDataSource implements GasolineraDataSource {

    private static final String DEFAULT_FUEL_KEY = "Precio Gasoleo A"; // igual que el flujo remoto actual

    private final Context context;

    public LocalJsonDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public List<Gasolinera> loadGasolineras() throws Exception {

        String json = readRawJson();
        if (json == null || json.isEmpty()) return new ArrayList<>();

        // ✅ Usamos el MISMO parser que remoto/caché para mantener coherencia
        return GasolineraJsonParser.parse(json, DEFAULT_FUEL_KEY);
    }

    private String readRawJson() throws Exception {
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(R.raw.gasolineras_spain);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024]; // 16KB
            int read;

            while ((read = is.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }

            return bos.toString(StandardCharsets.UTF_8.name());

        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
    }
}