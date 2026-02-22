package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class GasolineraRepository {

    private final GasolineraDataSource dataSource;

    public GasolineraRepository(GasolineraDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Gasolinera> getGasolineras() throws Exception {
        return dataSource.loadGasolineras();
    }
}