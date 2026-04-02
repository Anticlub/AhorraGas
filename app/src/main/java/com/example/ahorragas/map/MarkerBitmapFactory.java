package com.example.ahorragas.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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

    public static Bitmap createMarker(Context context,
                                      Gasolinera gasolinera,
                                      FuelType fuelType) {
        String priceText = gasolinera.getFormattedPrice(fuelType);
        int logoResId    = BrandLogoProvider.getLogoResId(gasolinera.getMarca());
        String key       = logoResId + "|" + gasolinera.getPriceLevel().name() + "|" + priceText;

        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;

        Bitmap rendered = renderMarker(context, gasolinera, priceText, logoResId);
        CACHE.put(key, rendered);
        return rendered;
    }

    private static Bitmap renderMarker(Context context,
                                       Gasolinera gasolinera,
                                       String priceText,
                                       int logoResId) {
        float density = context.getResources().getDisplayMetrics().density;

        // ── Reducción ~12% respecto al original ──
        // Original: width=74, bubbleHeight=56, pinHeight=14, corner=10, logoRadius=13
        int width = px(density, 52);
        int bubbleHeight = px(density, 40);
        int pinHeight = px(density, 10);
        int height = bubbleHeight + pinHeight;
        int corner = px(density, 8);

        int bgColor = getPriceLevelColor(gasolinera.getPriceLevel());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint   = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Burbuja de fondo
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        RectF bubble = new RectF(0, 0, width, bubbleHeight);
        canvas.drawRoundRect(bubble, corner, corner, paint);

        // Borde blanco
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(density * 1.6f);
        canvas.drawRoundRect(bubble, corner, corner, paint);

        // Pin inferior
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        float pinWidth = px(density, 8);
        Path pin = new Path();
        pin.moveTo(width / 2f - pinWidth, bubbleHeight - density);
        pin.lineTo(width / 2f + pinWidth, bubbleHeight - density);
        pin.lineTo(width / 2f, height);
        pin.close();
        canvas.drawPath(pin, paint);

        // Círculo blanco para el logo
        float centerX  = width / 2f;
        float centerY  = bubbleHeight * 0.40f;
        int logoRadius = px(density, 9);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, logoRadius, paint);

        // Logo dentro del círculo
        drawLogo(context, canvas, logoResId, centerX, centerY, logoRadius);

        // Texto del precio
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

        clipPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        circularCanvas.drawBitmap(scaled, 0, 0, clipPaint);

        Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(circular, cx - logoSize / 2f, cy - logoSize / 2f, drawPaint);
    }

    private static int px(float density, int dpValue) {
        return Math.round(density * dpValue);
    }
}