package com.ahorragas.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class MunicipioQueryTest {

    // ── stripLeadingArticle ─────────────────────────────────────────────────

    @Test
    public void stripLeadingArticle_null_returnsEmpty() {
        assertEquals("", MunicipioQuery.stripLeadingArticle(null));
    }

    @Test
    public void stripLeadingArticle_removesLeadingArticleKeepingCase() {
        assertEquals("Ejido", MunicipioQuery.stripLeadingArticle("El Ejido"));
        assertEquals("Coruña", MunicipioQuery.stripLeadingArticle("La Coruña"));
        assertEquals("Palmas", MunicipioQuery.stripLeadingArticle("Las Palmas"));
    }

    @Test
    public void stripLeadingArticle_caseInsensitiveDetection() {
        // La detección del artículo es insensible a mayúsculas
        assertEquals("Ejido", MunicipioQuery.stripLeadingArticle("EL Ejido"));
    }

    @Test
    public void stripLeadingArticle_noArticle_returnsTrimmed() {
        assertEquals("Sevilla", MunicipioQuery.stripLeadingArticle("  Sevilla  "));
    }

    @Test
    public void stripLeadingArticle_doesNotStripWhenNotStandaloneArticle() {
        // "Elche" empieza por "el" pero no por "el " (con espacio), no debe recortar
        assertEquals("Elche", MunicipioQuery.stripLeadingArticle("Elche"));
    }

    // ── invertedVariants ────────────────────────────────────────────────────

    @Test
    public void invertedVariants_null_returnsEmpty() {
        assertTrue(MunicipioQuery.invertedVariants(null).isEmpty());
    }

    @Test
    public void invertedVariants_noArticle_returnsEmpty() {
        assertTrue(MunicipioQuery.invertedVariants("Madrid").isEmpty());
    }

    @Test
    public void invertedVariants_withArticle_returnsBothForms() {
        List<String> variants = MunicipioQuery.invertedVariants("El Ejido");
        assertEquals(2, variants.size());
        assertEquals("Ejido (El)", variants.get(0));
        assertEquals("Ejido, El", variants.get(1));
    }

    @Test
    public void invertedVariants_articleA_isHandled() {
        List<String> variants = MunicipioQuery.invertedVariants("A Coruña");
        assertEquals(2, variants.size());
        assertEquals("Coruña (A)", variants.get(0));
        assertEquals("Coruña, A", variants.get(1));
    }
}
