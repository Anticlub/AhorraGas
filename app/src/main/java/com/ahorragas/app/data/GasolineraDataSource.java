package com.ahorragas.app.data;

import com.ahorragas.app.model.Gasolinera;

import java.util.List;

public interface GasolineraDataSource {

    List<Gasolinera> loadGasolineras() throws Exception;

}