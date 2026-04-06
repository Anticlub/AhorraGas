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

public final class MarkerBitmapFactory {

    private static final int CACHE_SIZE = 40;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(CACHE_SIZE);

    private MarkerBitmapFactory() {}

    public static void clearCache() {
        CACHE.evictAll();
    }

    public static int getBrandColor(String brand) {
        if (brand == null) return Color.parseColor("#607D8B");
        switch (brand.toLowerCase()) {
            case "repsol":    return Color.parseColor("#EF3340");
            case "cepsa":
            case "moeve":     return Color.parseColor("#FF6600");
            case "bp":        return Color.parseColor("#009900");
            case "shell":     return Color.parseColor("#DD1D21");
            case "galp":      return Color.parseColor("#FF6B00");
            case "petronor":  return Color.parseColor("#003087");
            case "carrefour": return Color.parseColor("#003CA6");
            case "alcampo":   return Color.parseColor("#1976D2");
            case "avia":      return Color.parseColor("#E31837");
            case "ballenoil":
            case "petroprix":
            case "plenergy":  return Color.parseColor("#455A64");
            default:          return Color.parseColor("#607D8B");
        }
    }

    public static int getPriceLevelColor(PriceLevel level) {
        if (level == null) return Color.parseColor("#757575");
        switch (level) {
            case CHEAP:     return Color.parseColor("#388E3C");
            case MID:       return Color.parseColor("#F57C00");
            case EXPENSIVE: return Color.parseColor("#D32F2F");
            case UNKNOWN:
            default:        return Color.parseColor("#757575");
        }
    }
    /**
     * Devuelve el color azul eléctrico para marcadores de electrolineras.
     */
    public static int getElectricColor() {
        return Color.parseColor("#1565C0");
    }

    /**
     * Crea el bitmap del marcador con precio y nivel de precio explícitos.
     * Usar cuando el precio mostrado difiere del original (p.ej. con descuento).
     *
     * @param context           Contexto de la aplicación.
     * @param gasolinera        Gasolinera a representar.
     * @param fuelType          Tipo de combustible seleccionado.
     * @param overridePriceText Texto de precio a mostrar, o null para usar el precio original.
     * @param priceLevel        Nivel de precio a usar para el color del marcador.
     * @return Bitmap del marcador.
     */
    public static Bitmap createMarker(Context context,
                                      Gasolinera gasolinera,
                                      FuelType fuelType) {
        String priceText;
        int bgColor;

        if (gasolinera.isElectric()) {
            // Para electrolineras mostramos la potencia máxima
            priceText = getMaxPotenciaLabel(gasolinera);
            bgColor   = getElectricColor();
        } else {
            priceText = gasolinera.getFormattedPrice(fuelType);
            bgColor   = getPriceLevelColor(gasolinera.getPriceLevel());
        }

        int logoResId = gasolinera.isElectric()
                ? BrandLogoProvider.getLogoResId(gasolinera.getMarca(), gasolinera.getOperador())
                : BrandLogoProvider.getLogoResId(gasolinera.getMarca());
        String key    = logoResId + "|" + (gasolinera.isElectric() ? "electric" :
                gasolinera.getPriceLevel().name()) + "|" + priceText;

        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;

        Bitmap rendered = renderMarker(context, gasolinera, priceText, logoResId, bgColor);
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
                                       int bgColor) {
        float density = context.getResources().getDisplayMetrics().density;

        int width        = px(density, 52);
        int bubbleHeight = px(density, 40);
        int pinHeight    = px(density, 10);
        int height       = bubbleHeight + pinHeight;
        int corner       = px(density, 8);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint   = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        RectF bubble = new RectF(0, 0, width, bubbleHeight);
        canvas.drawRoundRect(bubble, corner, corner, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(density * 1.6f);
        canvas.drawRoundRect(bubble, corner, corner, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        float pinWidth = px(density, 8);
        Path pin = new Path();
        pin.moveTo(width / 2f - pinWidth, bubbleHeight - density);
        pin.lineTo(width / 2f + pinWidth, bubbleHeight - density);
        pin.lineTo(width / 2f, height);
        pin.close();
        canvas.drawPath(pin, paint);

        float centerX  = width / 2f;
        float centerY  = bubbleHeight * 0.40f;
        int logoRadius = px(density, 9);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, logoRadius, paint);

        drawLogo(context, canvas, logoResId, centerX, centerY, logoRadius);

        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setFakeBoldText(false);
        paint.setTextSize(density * 7f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(priceText, centerX, bubbleHeight * 0.83f, paint);

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