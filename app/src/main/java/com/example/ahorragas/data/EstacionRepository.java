package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EstacionRepository {

    private static EstacionRepository instance;

    private final GasolineraRepository gasolineraRepository;
    private final ElectrolineraRepository electrolineraRepository;

    private EstacionRepository(GasolineraRepository gasolineraRepository,
                               ElectrolineraRepository electrolineraRepository) {
        this.gasolineraRepository    = gasolineraRepository;
        this.electrolineraRepository = electrolineraRepository;
    }

    /**
     * Devuelve la instancia única del repositorio unificado.
     *
     * @param gasolineraRepository    repositorio de gasolineras
     * @param electrolineraRepository repositorio de electrolineras
     * @return instancia singleton de EstacionRepository
     */
    public static synchronized EstacionRepository getInstance(
            GasolineraRepository gasolineraRepository,
            ElectrolineraRepository electrolineraRepository) {
        if (instance == null) {
            instance = new EstacionRepository(gasolineraRepository, electrolineraRepository);
        }
        return instance;
    }

    /**
     * Descarga gasolineras y electrolineras en paralelo y devuelve
     * una lista unificada de {@link Gasolinera}.
     * Las electrolineras se mapean a Gasolinera con isElectric=true.
     *
     * @return lista combinada de gasolineras y electrolineras
     * @throws RepoError si ambas fuentes fallan simultáneamente
     */
    public List<Gasolinera> getEstaciones() throws RepoError {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<List<Gasolinera>> futureGasolineras = executor.submit(
                new Callable<List<Gasolinera>>() {
                    @Override
                    public List<Gasolinera> call() throws Exception {
                        return gasolineraRepository.getGasolineras();
                    }
                });

        Future<List<Gasolinera>> futureElectrolineras = executor.submit(
                new Callable<List<Gasolinera>>() {
                    @Override
                    public List<Gasolinera> call() throws Exception {
                        List<Electrolinera> electrolineras =
                                electrolineraRepository.getElectrolineras();
                        return mapElectrolinerasToGasolineras(electrolineras);
                    }
                });

        executor.shutdown();

        List<Gasolinera> result = new ArrayList<>();
        RepoError gasolineraError   = null;
        RepoError electrolineraError = null;

        try {
            result.addAll(futureGasolineras.get());
        } catch (Exception e) {
            gasolineraError = new RepoError(RepoError.Type.NETWORK,
                    "Error cargando gasolineras: " + e.getMessage());
        }

        try {
            result.addAll(futureElectrolineras.get());
        } catch (Exception e) {
            electrolineraError = new RepoError(RepoError.Type.NETWORK,
                    "Error cargando electrolineras: " + e.getMessage());
        }

        // Solo lanzamos error si AMBAS fuentes han fallado
        if (gasolineraError != null && electrolineraError != null) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Error cargando gasolineras y electrolineras");
        }

        return result;
    }

    public void clearMemoryCache() {
        gasolineraRepository.clearMemoryCache();
        electrolineraRepository.clearMemoryCache();
    }

    /**
     * Mapea una lista de {@link Electrolinera} a una lista de {@link Gasolinera}
     * con los campos eléctricos rellenos e isElectric=true.
     *
     * @param electrolineras lista de electrolineras a mapear
     * @return lista de Gasolinera con datos eléctricos
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

            // Precio null para todos los FuelType excepto ELECTRICO
            // que usamos como flag de identificación en el filtrado
            g.setPrecio(FuelType.ELECTRICO, 0.0);

            result.add(g);
        }

        return result;
    }
}