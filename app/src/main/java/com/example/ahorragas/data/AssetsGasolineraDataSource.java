package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.model.Gasolinera;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AssetsGasolineraDataSource implements GasolineraDataSource {

    private static final String ASSETS_FILE = "gasolineras.json";

    private final Context context;

    public AssetsGasolineraDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Carga las gasolineras desde el fichero precargado en assets/.
     *
     * @return lista de gasolineras
     * @throws RepoError si hay error de lectura o parseo
     */
    @Override
    public List<Gasolinera> loadGasolineras() throws RepoError {
        try {
            String json = readAsset(ASSETS_FILE);
            return GasolineraJsonParser.parse(json);
        } catch (RepoError e) {
            throw e;
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.PARSE,
                    "Error leyendo gasolineras desde assets: " + e.getMessage());
        }
    }

    private String readAsset(String fileName) throws Exception {
        InputStream is = context.getAssets().open(fileName);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8), 16384);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[8192];
        int charsRead;
        while ((charsRead = reader.read(buffer, 0, buffer.length)) != -1) {
            sb.append(buffer, 0, charsRead);
        }
        reader.close();
        return sb.toString();
    }
}