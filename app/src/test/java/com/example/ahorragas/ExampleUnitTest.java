package com.example.ahorragas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceLevel;
import com.example.ahorragas.model.PriceRange;
import com.example.ahorragas.util.GasolineraSorter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ExampleUnitTest {

    @Test
    public void sortByDistance_placesNearestFirst() {
        Gasolinera a = new Gasolinera(1, "A", "Madrid", "Calle A", 40.4168, -3.7038, 1.500);
        Gasolinera b = new Gasolinera(2, "B", "Madrid", "Calle B", 40.4500, -3.7000, 1.400);

        List<Gasolinera> sorted = GasolineraSorter.filterComputeAndSort(
                Arrays.asList(b, a),
                40.4168,
                -3.7038
        );

        assertEquals(1, sorted.get(0).getId());
        assertTrue(sorted.get(0).getDistanceMeters() <= sorted.get(1).getDistanceMeters());
    }

    @Test
    public void calculatePriceLevel_marksCheapestAndExpensive() {
        Gasolinera cheap = new Gasolinera(1, "Cheap", "Madrid", "Calle 1", 40.0, -3.0, null);
        cheap.setPrecio(FuelType.GASOLEO_A, 1.200);

        Gasolinera mid = new Gasolinera(2, "Mid", "Madrid", "Calle 2", 40.1, -3.1, null);
        mid.setPrecio(FuelType.GASOLEO_A, 1.350);

        Gasolinera expensive = new Gasolinera(3, "Expensive", "Madrid", "Calle 3", 40.2, -3.2, null);
        expensive.setPrecio(FuelType.GASOLEO_A, 1.500);

        List<Gasolinera> stations = Arrays.asList(cheap, mid, expensive);
        PriceRange range = GasolineraSorter.calculatePriceRange(stations, FuelType.GASOLEO_A);

        assertEquals(PriceLevel.CHEAP, GasolineraSorter.getPriceLevel(cheap.getPrecio(FuelType.GASOLEO_A), range));
        assertEquals(PriceLevel.MID, GasolineraSorter.getPriceLevel(mid.getPrecio(FuelType.GASOLEO_A), range));
        assertEquals(PriceLevel.EXPENSIVE, GasolineraSorter.getPriceLevel(expensive.getPrecio(FuelType.GASOLEO_A), range));
    }
}
