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

    private EstacionRepository(GasolineraRepository gasolineraRepository,
                               ElectrolineraRepository electrolineraRepository) {
        this.gasolineraRepository    = gasolineraRepository;
        this.electrolineraRepository = electrolineraRepository;
    }

    /**
     * Devuelve la instancia única del repositorio unificado.
     */
    public static synchronized EstacionRepository getInstance(
            GasolineraRepository gasolineraRepository,
            ElectrolineraRepository electrolineraRepository) {
        if (instance == null) {
            instance = new EstacionRepository(
                    gasolineraRepository, electrolineraRepository);
        }
        return instance;
    }

    /**
     * Devuelve gasolineras desde Room o red si no hay datos locales.
     *
     * @return lista de gasolineras
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getGasolineras() throws RepoError {
        return gasolineraRepository.getGasolineras();
    }

    /**
     * Devuelve electrolineras desde Room o red si no hay datos locales,
     * mapeadas a {@link Gasolinera}.
     *
     * @return lista de electrolineras mapeadas
     * @throws RepoError si hay fallo
     */
    public List<Gasolinera> getElectrolineras() throws RepoError {
        List<Electrolinera> electrolineras = electrolineraRepository.getElectrolineras();
        return mapElectrolinerasToGasolineras(electrolineras);
    }

    /**
     * Invalida las cachés en memoria de ambos repositorios.
     */
    public void clearMemoryCache() {
        gasolineraRepository.clearMemoryCache();
        electrolineraRepository.clearMemoryCache();
    }

    /**
     * Mapea una lista de {@link Electrolinera} a {@link Gasolinera} con isElectric=true.
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