package com.example.ahorragas.map;

import com.example.ahorragas.R;

import java.util.HashMap;
import java.util.Map;

public final class BrandLogoProvider {

    private static final Map<String, Integer> LOGO_MAP = new HashMap<>();

    static {
        LOGO_MAP.put("repsol", R.drawable.logo_repsol);
        LOGO_MAP.put("cepsa", R.drawable.logo_cepsa);
        LOGO_MAP.put("moeve", R.drawable.logo_cepsa);
        LOGO_MAP.put("bp", R.drawable.logo_bp);
        LOGO_MAP.put("shell", R.drawable.logo_shell);
        LOGO_MAP.put("galp", R.drawable.logo_galp);
        LOGO_MAP.put("petronor", R.drawable.logo_petronor);
        LOGO_MAP.put("carrefour", R.drawable.logo_carrefour);
        LOGO_MAP.put("alcampo", R.drawable.logo_alcampo);
        LOGO_MAP.put("avia", R.drawable.logo_avia);
        LOGO_MAP.put("ballenoil", R.drawable.logo_ballenoil);
        LOGO_MAP.put("petroprix", R.drawable.logo_petroprix);
        LOGO_MAP.put("plenergy", R.drawable.logo_plenergy);
    }

    private BrandLogoProvider() {
    }
    public static int getLogoResId(String marca) {
        if (marca == null || marca.trim().isEmpty()) {
            return R.drawable.logo_generic;
        }

        String key = marca.trim().toLowerCase();

        Integer resId = LOGO_MAP.get(key);
        if (resId != null) return resId;

        for (Map.Entry<String, Integer> entry : LOGO_MAP.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return R.drawable.logo_generic;
    }

    /**
     * Comprueba si la marca tiene un logo específico (no genérico).
     */
    public static boolean hasSpecificLogo(String marca) {
        return getLogoResId(marca) != R.drawable.logo_generic;
    }
}
