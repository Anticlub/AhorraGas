package com.example.ahorragas.data;

import com.example.ahorragas.model.Electrolinera;
import java.util.List;

public interface ElectrolineraDataSource {

    /**
     * Carga la lista de electrolineras desde la fuente de datos.
     *
     * @return lista de electrolineras, nunca null
     * @throws RepoError si hay fallo de red, parseo o respuesta vacía
     */
    List<Electrolinera> loadElectrolineras() throws RepoError;
}