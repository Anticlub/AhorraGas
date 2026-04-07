package com.example.ahorragas.data;

import android.content.Context;

import com.example.ahorragas.model.Electrolinera;

import java.io.InputStream;
import java.util.List;

/**
 * Carga las electrolineras desde el fichero XML precargado en assets/.
 * Se usa en la primera instalación para evitar la descarga de red.
 */
public class AssetsElectrolineraDataSource implements ElectrolineraDataSource {

    private static final String ASSETS_FILE = "electrolineras.xml";

    private final Context context;

    public AssetsElectrolineraDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Carga las electrolineras desde el fichero precargado en assets/.
     *
     * @return lista de electrolineras
     * @throws RepoError si hay error de lectura o parseo
     */
    @Override
    public List<Electrolinera> loadElectrolineras() throws RepoError {
        try {
            InputStream is = context.getAssets().open(ASSETS_FILE);
            RemoteDgtDataSource parser = new RemoteDgtDataSource();
            List<Electrolinera> result = parser.parseXmlPublic(is);
            is.close();
            if (result == null || result.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                        "El fichero de assets de electrolineras está vacío");
            }
            return result;
        } catch (RepoError e) {
            throw e;
        } catch (Exception e) {
            throw new RepoError(RepoError.Type.PARSE,
                    "Error leyendo electrolineras desde assets: " + e.getMessage());
        }
    }
}