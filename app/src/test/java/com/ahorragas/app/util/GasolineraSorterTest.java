package com.ahorragas.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceLevel;
import com.ahorragas.app.model.PriceRange;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GasolineraSorterTest {

    private static Gasolinera diesel(int id, double price) {
        Gasolinera g = new Gasolinera();
        g.setId(id);
        g.setLat(40.0);
        g.setLon(-3.0);
        g.setElectric(false);
        g.setPrecio(FuelType.GASOLEO_A, price);
        return g;
    }

    private static Gasolinera electrica(int id) {
        Gasolinera g = new Gasolinera();
        g.setId(id);
        g.setLat(40.0);
        g.setLon(-3.0);
        g.setElectric(true);
        return g;
    }

    // ── filterByFuel ────────────────────────────────────────────────────────

    @Test
    public void filterByFuel_null_returnsEmpty() {
        assertTrue(GasolineraSorter.filterByFuel(null, FuelType.GASOLEO_A).isEmpty());
    }

    @Test
    public void filterByFuel_diesel_returnsOnlyNonElectricWithThatPrice() {
        Gasolinera d = diesel(1, 1.5);
        Gasolinera e = electrica(2);
        Gasolinera sinPrecio = diesel(3, 0.0); // precio 0 -> no cuenta como que tiene precio

        List<Gasolinera> result = GasolineraSorter.filterByFuel(
                Arrays.asList(d, e, sinPrecio), FuelType.GASOLEO_A);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    public void filterByFuel_electrico_returnsOnlyElectric() {
        List<Gasolinera> result = GasolineraSorter.filterByFuel(
                Arrays.asList(diesel(1, 1.5), electrica(2)), FuelType.ELECTRICO);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getId());
    }

    @Test
    public void filterByFuel_invalidCoordinates_areExcluded() {
        Gasolinera bad = diesel(9, 1.5);
        bad.setLat(0.0);
        bad.setLon(0.0); // 0/0 = coordenada inválida

        assertTrue(GasolineraSorter.filterByFuel(
                Collections.singletonList(bad), FuelType.GASOLEO_A).isEmpty());
    }

    // ── filterByBrand ───────────────────────────────────────────────────────

    private static Gasolinera conMarca(int id, String marca) {
        Gasolinera g = new Gasolinera();
        g.setId(id);
        g.setMarca(marca);
        return g;
    }

    @Test
    public void filterByBrand_null_returnsEmpty() {
        assertTrue(GasolineraSorter.filterByBrand(null, "bp").isEmpty());
    }

    @Test
    public void filterByBrand_nullBrand_returnsAll() {
        List<Gasolinera> list = Arrays.asList(conMarca(1, "Repsol"), conMarca(2, "BP OIL"));
        assertEquals(2, GasolineraSorter.filterByBrand(list, null).size());
    }

    @Test
    public void filterByBrand_keepsOnlyMatchingBrandCaseInsensitive() {
        List<Gasolinera> list = Arrays.asList(
                conMarca(1, "BP OIL ESPAÑA"), conMarca(2, "Repsol"), conMarca(3, "bp local"));

        List<Gasolinera> result = GasolineraSorter.filterByBrand(list, "bp");

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(3, result.get(1).getId());
    }

    @Test
    public void filterByBrand_ignoresNullMarca() {
        List<Gasolinera> list = Arrays.asList(conMarca(1, null), conMarca(2, "Repsol"));
        List<Gasolinera> result = GasolineraSorter.filterByBrand(list, "repsol");
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getId());
    }

    // ── calculatePriceRange ─────────────────────────────────────────────────

    @Test
    public void calculatePriceRange_emptyOrNull_isEmpty() {
        assertTrue(GasolineraSorter.calculatePriceRange(null, FuelType.GASOLEO_A).isEmpty());
        assertTrue(GasolineraSorter.calculatePriceRange(
                Collections.emptyList(), FuelType.GASOLEO_A).isEmpty());
    }

    @Test
    public void calculatePriceRange_computesMinMaxIgnoringNonPositive() {
        List<Gasolinera> list = Arrays.asList(
                diesel(1, 1.2), diesel(2, 1.5), diesel(3, 1.8), diesel(4, 0.0));

        PriceRange range = GasolineraSorter.calculatePriceRange(list, FuelType.GASOLEO_A);

        assertEquals(1.2, range.getMin(), 1e-9);
        assertEquals(1.8, range.getMax(), 1e-9);
    }

    // ── getPriceLevel ───────────────────────────────────────────────────────

    @Test
    public void getPriceLevel_nullOrEmptyRange_isUnknown() {
        assertEquals(PriceLevel.UNKNOWN,
                GasolineraSorter.getPriceLevel(null, new PriceRange(1.0, 2.0, 2)));
        assertEquals(PriceLevel.UNKNOWN,
                GasolineraSorter.getPriceLevel(1.5, new PriceRange(null, null, 0)));
    }

    @Test
    public void getPriceLevel_sameMinMax_isMid() {
        assertEquals(PriceLevel.MID,
                GasolineraSorter.getPriceLevel(1.5, new PriceRange(1.5, 1.5, 3)));
    }

    @Test
    public void getPriceLevel_classifiesByNormalizedPosition() {
        PriceRange range = new PriceRange(1.0, 2.0, 5); // rango [1.0, 2.0]
        assertEquals(PriceLevel.CHEAP, GasolineraSorter.getPriceLevel(1.2, range));     // norm 0.2
        assertEquals(PriceLevel.MID, GasolineraSorter.getPriceLevel(1.5, range));       // norm 0.5
        assertEquals(PriceLevel.EXPENSIVE, GasolineraSorter.getPriceLevel(1.9, range)); // norm 0.9
    }
}
