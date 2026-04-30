package com.example.ahorragas.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.LruCache;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceLevel;
import com.example.ahorragas.util.FavoritesPrefs;

public final class MarkerBitmapFactory {
    // ── Dimensiones del marker (en dp) ──────────────────────────────────────────
    private static final int PILL_WIDTH_DP       = 72;
    private static final int PILL_HEIGHT_DP      = 26;
    private static final int PIN_HEIGHT_DP       = 8;
    private static final int PIN_WIDTH_DP        = 7;
    private static final int BORDER_WIDTH_DP     = 2;
    private static final int STAR_SIZE_DP        = 22;
    private static final int LOGO_MARGIN_DP      = 3;
    private static final int LOGO_MARGIN_LEFT_DP = 3;
    private static final int STAR_PADDING_DP     = 2;

    // ── Tipografía (en sp, usados como dp en Canvas) ────────────────────────────
    private static final float PRICE_TEXT_SIZE_SP = 10f;
    private static final float STAR_TEXT_SIZE_SP  = 22f;

    // ── Colores ──────────────────────────────────────────────────────────────────
    private static final String COLOR_MARKER_BG   = "#1A1A1A";
    private static final String COLOR_STAR        = "#FFD700";
    private static final String COLOR_BRAND_DEFAULT = "#607D8B";
    private static final String COLOR_CHEAP       = "#388E3C";
    private static final String COLOR_MID         = "#F57C00";
    private static final String COLOR_EXPENSIVE   = "#D32F2F";
    private static final String COLOR_UNKNOWN     = "#757575";
    private static final String COLOR_ELECTRIC    = "#1565C0";

    private static final int CACHE_SIZE = 40;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(CACHE_SIZE);

    private MarkerBitmapFactory() {}

    public static void clearCache() {
        CACHE.evictAll();
    }

    public static int getBrandColor(String brand) {
        if (brand == null) return Color.parseColor(COLOR_BRAND_DEFAULT);
        return switch (brand.toLowerCase()) {
            case "repsol" -> Color.parseColor("#EF3340");
            case "cepsa", "moeve" -> Color.parseColor("#FF6600");
            case "bp" -> Color.parseColor("#009900");
            case "shell" -> Color.parseColor("#DD1D21");
            case "galp" -> Color.parseColor("#FF6B00");
            case "petronor" -> Color.parseColor("#003087");
            case "carrefour" -> Color.parseColor("#003CA6");
            case "alcampo" -> Color.parseColor("#1976D2");
            case "avia" -> Color.parseColor("#E31837");
            case "ballenoil", "petroprix", "plenergy" -> Color.parseColor("#455A64");
            default -> Color.parseColor(COLOR_BRAND_DEFAULT);
        };
    }

    public static int getPriceLevelColor(PriceLevel level) {
        if (level == null) return Color.parseColor(COLOR_UNKNOWN);
        return switch (level) {
            case CHEAP -> Color.parseColor(COLOR_CHEAP);
            case MID -> Color.parseColor(COLOR_MID);
            case EXPENSIVE -> Color.parseColor(COLOR_EXPENSIVE);
            default -> Color.parseColor(COLOR_UNKNOWN);
        };
    }
    /**
     * Devuelve el color azul eléctrico para marcadores de electrolineras.
     */

    public static int getElectricColor() {
        return Color.parseColor(COLOR_ELECTRIC);
    }

    /**
     * Crea el bitmap del marcador con precio y nivel de precio calculados automáticamente.
     *
     * @param context    Contexto de la aplicación.
     * @param gasolinera Gasolinera a representar.
     * @param fuelType   Tipo de combustible seleccionado.
     * @return Bitmap del marcador.
     */
    public static Bitmap createMarker(Context context,
                                      Gasolinera gasolinera,
                                      FuelType fuelType) {
        String priceText;
        int bgColor;

        if (gasolinera.isElectric()) {
            priceText = getMaxPotenciaLabel(gasolinera);
            bgColor   = getElectricColor();
        } else {
            priceText = gasolinera.getFormattedPrice(fuelType);
            bgColor   = getPriceLevelColor(gasolinera.getPriceLevel());
        }

        boolean isFavourite = FavoritesPrefs.isFavorite(context, gasolinera);

        int logoResId = gasolinera.isElectric()
                ? BrandLogoProvider.getLogoResId(gasolinera.getMarca(), gasolinera.getOperador())
                : BrandLogoProvider.getLogoResId(gasolinera.getMarca());
        String key = logoResId + "|" + (gasolinera.isElectric() ? "electric" :
                gasolinera.getPriceLevel().name()) + "|" + priceText + "|" + isFavourite;

        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;

        Bitmap rendered = renderMarker(context, gasolinera, priceText, logoResId, bgColor, isFavourite);
        CACHE.put(key, rendered);
        return rendered;
    }

    /**
     * Crea el bitmap del marcador con un texto de precio personalizado.
     * Útil cuando el precio mostrado difiere del original (p.ej. con descuento).
     *
     * @param context    Contexto de la aplicación.
     * @param gasolinera Gasolinera a representar.
     * @param fuelType   Tipo de combustible seleccionado.
     * @param priceText  Texto de precio a mostrar en el marker.
     * @param priceLevel Nivel de precio para el color del marker.
     * @return Bitmap del marcador.
     */
    public static Bitmap createMarker(Context context,
                                      Gasolinera gasolinera,
                                      FuelType fuelType,
                                      String priceText,
                                      PriceLevel priceLevel) {
        int bgColor = gasolinera.isElectric()
                ? getElectricColor()
                : getPriceLevelColor(priceLevel);

        boolean isFavourite = FavoritesPrefs.isFavorite(context, gasolinera);

        int logoResId = gasolinera.isElectric()
                ? BrandLogoProvider.getLogoResId(gasolinera.getMarca(), gasolinera.getOperador())
                : BrandLogoProvider.getLogoResId(gasolinera.getMarca());
        String key = logoResId + "|" + priceLevel.name() + "|" + priceText + "|" + isFavourite;

        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;

        Bitmap rendered = renderMarker(context, gasolinera, priceText, logoResId, bgColor, isFavourite);
        CACHE.put(key, rendered);
        return rendered;
    }

    /**
     * Devuelve la potencia máxima de una electrolinera formateada para el marcador.
     * Ejemplo: "350kW"
     *
     * @param gasolinera electrolinera de la que obtener la potencia
     * @return string con la potencia o "EV" si no hay datos
     */
    private static String getMaxPotenciaLabel(Gasolinera gasolinera) {
        if (gasolinera.getConectores() == null || gasolinera.getConectores().isEmpty()) return "EV";
        double maxW = 0;
        for (com.example.ahorragas.model.Electrolinera.Conector c : gasolinera.getConectores()) {
            if (c.getPotenciaW() != null && c.getPotenciaW() > maxW) {
                maxW = c.getPotenciaW();
            }
        }
        if (maxW <= 0) return "EV";
        return String.format(java.util.Locale.getDefault(), "%.0fkW", maxW / 1000.0);
    }

    private static Bitmap renderMarker(Context context,
                                       Gasolinera gasolinera,
                                       String priceText,
                                       int logoResId,
                                       int bgColor,
                                       boolean isFavourite) {
        float density = context.getResources().getDisplayMetrics().density;

        int starSize    = isFavourite ? px(density, STAR_SIZE_DP)    : 0;
        int starPadding = isFavourite ? px(density, STAR_PADDING_DP) : 0;

        int pillWidth   = px(density, PILL_WIDTH_DP);
        int pillHeight  = px(density, PILL_HEIGHT_DP);
        int pinHeight   = px(density, PIN_HEIGHT_DP);
        int cornerRadii = pillHeight / 2;
        int borderWidth = px(density, BORDER_WIDTH_DP);
        int pillTop     = starSize + starPadding;

        int totalHeight = pillHeight + pinHeight + starSize + starPadding;

        Bitmap bitmap = Bitmap.createBitmap(pillWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint   = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        RectF borderRect = new RectF(0, pillTop, pillWidth, pillTop + pillHeight);
        canvas.drawRoundRect(borderRect, cornerRadii, cornerRadii, paint);

        // ── Fondo negro interior ─────────────────────────────────────────────────
        paint.setColor(Color.parseColor(COLOR_MARKER_BG));
        RectF innerRect = new RectF(
                borderWidth,
                pillTop + borderWidth,
                pillWidth - borderWidth,
                pillTop + pillHeight - borderWidth);
        canvas.drawRoundRect(innerRect, cornerRadii - borderWidth, cornerRadii - borderWidth, paint);

        // ── Logo de marca ──────────────────────────────────
        int logoMargin       = px(density, LOGO_MARGIN_DP);
        int logoCircleRadius = (pillHeight / 2) - borderWidth - logoMargin;
        int logoMarginLeft = px(density, LOGO_MARGIN_LEFT_DP);
        float logoCx = borderWidth + logoMarginLeft + logoCircleRadius;
        float logoCy = pillTop + pillHeight / 2f;

        drawLogo(context, canvas, logoResId, logoCx, logoCy, logoCircleRadius);

        // ── Texto precio ─────────────────────────────────────────────────────────
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(density * PRICE_TEXT_SIZE_SP);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);

        float textX = logoCx + logoCircleRadius + (pillWidth - logoCx - logoCircleRadius) / 2f;
        float textY = pillTop + pillHeight / 2f - (paint.descent() + paint.ascent()) / 2f;
        canvas.drawText(priceText, textX, textY, paint);

        // ── Pin triangular ───────────────────────────────────────────────────────
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        float pinWidth = px(density, PIN_WIDTH_DP);
        float pinBase  = pillTop + pillHeight;
        Path pin = new Path();
        pin.moveTo(pillWidth / 2f - pinWidth, pinBase);
        pin.lineTo(pillWidth / 2f + pinWidth, pinBase);
        pin.lineTo(pillWidth / 2f, pinBase + pinHeight);
        pin.close();
        canvas.drawPath(pin, paint);

        // ── Estrella favorito ────────────────────────────────────────────────────
        if (isFavourite) {
            paint.setColor(Color.parseColor(COLOR_STAR));
            paint.setTextSize(density * STAR_TEXT_SIZE_SP);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(false);
            canvas.drawText("★", pillWidth / 2f, starSize, paint);
        }

        return bitmap;
    }

    private static void drawLogo(Context context, Canvas canvas,
                                 int logoResId, float cx, float cy, int radius) {
        Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), logoResId);
        if (logoBitmap == null) return;

        int logoSize = radius * 2;
        Bitmap scaled = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true);

        Bitmap circular = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888);
        Canvas circularCanvas = new Canvas(circular);

        Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circularCanvas.drawCircle(logoSize / 2f, logoSize / 2f, logoSize / 2f, clipPaint);

        clipPaint.setXfermode(new android.graphics.PorterDuffXfermode(
                android.graphics.PorterDuff.Mode.SRC_IN));
        circularCanvas.drawBitmap(scaled, 0, 0, clipPaint);

        Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(circular, cx - logoSize / 2f, cy - logoSize / 2f, drawPaint);
    }

    private static int px(float density, int dpValue) {
        return Math.round(density * dpValue);
    }
}