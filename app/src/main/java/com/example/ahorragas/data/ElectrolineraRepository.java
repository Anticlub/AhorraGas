package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;

import java.util.List;

public class ElectrolineraRepository {

    private static ElectrolineraRepository instance;
    private final ElectrolineraDataSource dataSource;
    private List<Electrolinera> memoryCache;

    private ElectrolineraRepository(ElectrolineraDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Devuelve la instancia única del repositorio.
     *
     * @param dataSource fuente de datos a usar si aún no existe la instancia
     * @return instancia singleton de ElectrolineraRepository
     */
    public static synchronized ElectrolineraRepository getInstance(
            ElectrolineraDataSource dataSource) {
        if (instance == null) {
            instance = new ElectrolineraRepository(dataSource);
        }
        return instance;
    }

    /**
     * Devuelve la lista de electrolineras desde caché en memoria o red.
     *
     * @return lista de electrolineras
     * @throws RepoError si hay fallo de red, parseo o respuesta vacía
     */
    public synchronized List<Electrolinera> getElectrolineras() throws RepoError {

        if (memoryCache != null) return memoryCache;

        memoryCache = dataSource.loadElectrolineras();

        if (memoryCache == null || memoryCache.isEmpty()) {
            throw new RepoError(RepoError.Type.EMPTY_RESPONSE,
                    "Sin datos de electrolineras");
        }

        return memoryCache;
    }

    public synchronized void clearMemoryCache() {
        memoryCache = null;
    }
}