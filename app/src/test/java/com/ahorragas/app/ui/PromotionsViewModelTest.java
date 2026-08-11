package com.ahorragas.app.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.ahorragas.app.model.PromotionPlan;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests de la lógica de filtrado por marca (alias operador CSV -> marca de la app).
 */
public class PromotionsViewModelTest {

    private PromotionsViewModel viewModel;

    private static PromotionPlan planFor(String operator) {
        return new PromotionPlan(operator, "Plan", "Descripción", "2026-12-31",
                5.0, PromotionPlan.DiscountType.OTHER, "todos");
    }

    @Before
    public void setUp() {
        viewModel = new PromotionsViewModel();
    }

    @Test
    public void filterByBrand_nullPlans_returnsNull() {
        assertNull(viewModel.filterByBrand(null, "repsol"));
    }

    @Test
    public void filterByBrand_nullOrEmptyBrand_returnsEmpty() {
        List<PromotionPlan> plans = Collections.singletonList(planFor("Repsol"));
        assertTrue(viewModel.filterByBrand(plans, null).isEmpty());
        assertTrue(viewModel.filterByBrand(plans, "  ").isEmpty());
    }

    @Test
    public void filterByBrand_usesAlias_bpMatchesBpOil() {
        List<PromotionPlan> plans = Arrays.asList(
                planFor("BP OIL España"), planFor("Repsol Comercial"));

        List<PromotionPlan> result = viewModel.filterByBrand(plans, "bp");

        assertEquals(1, result.size());
        assertEquals("BP OIL España", result.get(0).getOperator());
    }

    @Test
    public void filterByBrand_usesAlias_cepsaMatchesMoeve() {
        List<PromotionPlan> plans = Arrays.asList(
                planFor("Moeve Gas y Petróleo"), planFor("Galp Energía"));

        List<PromotionPlan> result = viewModel.filterByBrand(plans, "cepsa");

        assertEquals(1, result.size());
        assertEquals("Moeve Gas y Petróleo", result.get(0).getOperator());
    }

    @Test
    public void filterByBrand_noAlias_usesBrandNameItself() {
        List<PromotionPlan> plans = Arrays.asList(
                planFor("Petronor Energía"), planFor("Repsol"));

        List<PromotionPlan> result = viewModel.filterByBrand(plans, "petronor");

        assertEquals(1, result.size());
        assertEquals("Petronor Energía", result.get(0).getOperator());
    }
}
