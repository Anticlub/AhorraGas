package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public interface GasolineraDataSource {

    List<Gasolinera> loadGasolineras() throws Exception;

}