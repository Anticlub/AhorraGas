package com.ahorragas.app.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;

import org.junit.Test;

public class EstacionMapperTest {

    @Test
    public void toGasolinera_mapsFieldsAndPrices() {
        EstacionEntity e = new EstacionEntity();
        e.stationId = "42";
        e.marca = "Repsol";
        e.municipio = "Madrid";
        e.direccion = "Calle Mayor";
        e.lat = 40.4168;
        e.lon = -3.7038;
        e.precioGasoleoA = 1.5;
        e.precioGasolina95 = 1.6;

        Gasolinera g = EstacionMapper.toGasolinera(e);

        assertEquals(42, g.getId());
        assertEquals("Repsol", g.getMarca());
        assertEquals("Madrid", g.getMunicipio());
        assertEquals(40.4168, g.getLat(), 1e-9);
        assertEquals(-3.7038, g.getLon(), 1e-9);
        assertEquals(1.5, g.getPrecio(FuelType.GASOLEO_A), 1e-9);
        assertEquals(1.6, g.getPrecio(FuelType.GASOLINA_95_E5), 1e-9);
    }

    @Test
    public void toGasolinera_absentPrice_isNull() {
        EstacionEntity e = new EstacionEntity();
        e.stationId = "1";
        e.precioGasoleoA = 1.5;
        // precioGnc no se asigna -> null

        Gasolinera g = EstacionMapper.toGasolinera(e);

        assertNull(g.getPrecio(FuelType.GNC));
    }

    @Test
    public void toGasolinera_nonNumericStationId_doesNotCrash() {
        EstacionEntity e = new EstacionEntity();
        e.stationId = "no-numerico";
        e.marca = "X";

        // parseInt falla y se ignora: se mapea el resto sin excepción.
        Gasolinera g = EstacionMapper.toGasolinera(e);
        assertEquals("X", g.getMarca());
    }
}
