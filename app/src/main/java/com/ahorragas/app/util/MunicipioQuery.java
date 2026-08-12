package com.ahorragas.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helpers puros para normalizar los nombres de municipio introducidos por el
 * usuario y generar variantes de búsqueda para Room. Se extrae de MainActivity
 * para poder cubrirlo con tests unitarios.
 */
public final class MunicipioQuery {

    private static final String[] LEADING_ARTICLES =
            {"el ", "la ", "los ", "las ", "de ", "del "};

    private static final String[] INVERTED_ARTICLES =
            {"El ", "La ", "Los ", "Las ", "A "};

    private MunicipioQuery() {}

    /**
     * Si el municipio empieza por un artículo ("El Ejido", "La Coruña"…) devuelve
     * el resto sin el artículo (conservando el resto tal cual); si no, devuelve el
     * texto recortado. Nunca devuelve null.
     *
     * @param query texto introducido por el usuario.
     * @return palabra principal del municipio para buscar en Room.
     */
    public static String stripLeadingArticle(String query) {
        if (query == null) return "";
        String lower = query.trim().toLowerCase(Locale.getDefault());
        for (String article : LEADING_ARTICLES) {
            if (lower.startsWith(article)) {
                return query.trim().substring(article.length()).trim();
            }
        }
        return query.trim();
    }

    /**
     * Genera variantes "invertidas" para municipios con artículo, tal y como los
     * almacena la base de datos oficial ("El Ejido" → "Ejido (El)" / "Ejido, El").
     *
     * @param query texto introducido por el usuario.
     * @return lista de variantes, o lista vacía si no empieza por artículo.
     */
    public static List<String> invertedVariants(String query) {
        List<String> variants = new ArrayList<>();
        if (query == null) return variants;
        String trimmed = query.trim();
        for (String article : INVERTED_ARTICLES) {
            if (trimmed.startsWith(article)) {
                String rest = trimmed.substring(article.length()).trim();
                variants.add(rest + " (" + article.trim() + ")");
                variants.add(rest + ", " + article.trim());
                return variants;
            }
        }
        return variants;
    }
}
