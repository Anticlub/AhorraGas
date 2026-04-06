package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.List;

public class EstacionRepository {

    private static EstacionRepository instance;

    private final GasolineraRepository gasolineraRepository;
    private final ElectrolineraRepository electrolineraRepository;
    private final CachedRemoteApiDataSource cachedDataSource;

    /**
     * Callback para notificar actualizaciones en background.
     */
    public interface RefreshCallback {
        void onGasolinerasRefreshed(List<Gasolinera> updated);
        void onElectrolinerasRefreshed(List<Gasolinera> updated);
        void onRefreshError(Exception error);
    }

    private EstacionRepository(GasolineraRepository gasolineraRepository,
                               ElectrolineraRepository electrolineraRepository,
                               CachedRemoteApiDataSource cachedDataSource) {
        this.gasolineraRepository    = gasolineraRepository;
        this.electrolineraRepository = electrolineraRepository;
        this.cachedDataSource        = cachedDataSource;
    }

    /**
     * Devuelve la instancia única del repositorio unificado.
     */
    public static synchronized EstacionRepository getInstance(
            GasolineraRepository gasolineraRepository,
            ElectrolineraRepository electrolineraRepository,
            CachedRemoteApiDataSource cachedDataSource) {
        if (instance == null) {
            instance = new EstacionRepository(
                    gasolineraRepository, electrolineraRepository, cachedDataSource);
        }
        return instance;
    }

    /**
     * Devuelve gasolineras desde caché inmediatamente.
     * Si no hay caché, descarga de red (primera vez).
     *
     * @return lista de gasolineras
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getGasolineras() throws RepoError {
        return gasolineraRepository.getGasolineras();
    }

    /**
     * Devuelve electrolineras desde caché inmediatamente.
     * Si no hay caché, descarga de red (primera vez).
     *
     * @return lista de electrolineras mapeadas a Gasolinera
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getElectrolineras() throws RepoError {
        List<Electrolinera> electrolineras = electrolineraRepository.getElectrolineras();
        return mapElectrolinerasToGasolineras(electrolineras);
    }

    /**
     * Lanza actualizaciones en background para gasolineras y electrolineras.
     * Notifica via callback cuando lleguen datos frescos.
     *
     * @param refreshElectrolineras true si hay que refrescar también electrolineras
     * @param callback              notificado con los datos actualizados
     */
    public void refreshInBackground(boolean refreshElectrolineras, RefreshCallback callback) {
        // Refrescar gasolineras en background
        cachedDataSource.refreshInBackground(new CachedRemoteApiDataSource.RefreshCallback() {
            @Override
            public void onRefreshed(List<Gasolinera> updated) {
                // Actualizar caché del repositorio
                gasolineraRepository.clearMemoryCache();
                callback.onGasolinerasRefreshed(updated);
            }

            @Override
            public void onRefreshError(Exception error) {
                callback.onRefreshError(error);
            }
        });

        // Refrescar electrolineras en background si es necesario
        if (refreshElectrolineras) {
            electrolineraRepository.refreshInBackground(
                    new ElectrolineraRepository.RefreshCallback() {
                        @Override
                        public void onRefreshed(List<Electrolinera> updated) {
                            callback.onElectrolinerasRefreshed(
                                    mapElectrolinerasToGasolineras(updated));
                        }

                        @Override
                        public void onRefreshError(RepoError error) {
                            callback.onRefreshError(error);
                        }
                    });
        }
    }

    public void clearMemoryCache() {
        gasolineraRepository.clearMemoryCache();
        electrolineraRepository.clearMemoryCache();
    }

    /**
     * Mapea una lista de {@link Electrolinera} a una lista de {@link Gasolinera}
     * con los campos eléctricos rellenos e isElectric=true.
     */
    private List<Gasolinera> mapElectrolinerasToGasolineras(
            List<Electrolinera> electrolineras) {
        List<Gasolinera> result = new ArrayList<>();
        if (electrolineras == null) return result;

        for (Electrolinera e : electrolineras) {
            Gasolinera g = new Gasolinera();
            g.setMarca(e.getNombre());
            g.setOperador(e.getOperador());
            g.setDireccion(e.getDireccion());
            g.setMunicipio(e.getMunicipio());
            g.setHorario(e.getHorario());
            g.setLat(e.getLat());
            g.setLon(e.getLon());
            g.setElectric(true);
            g.setConectores(e.getConectores());
            g.setPrecio(FuelType.ELECTRICO, 0.0);
            result.add(g);
        }

        return result;
    }
}