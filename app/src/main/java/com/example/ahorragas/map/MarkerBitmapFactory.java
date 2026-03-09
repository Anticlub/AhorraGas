package com.example.ahorragas.map;

import android.content.Context;
import android.graphics.Bitmap;
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

    private MarkerBitmapFactory() {
    }

    public static void clearCache() {
        CACHE.evictAll();
    }

    public static int getBrandColor(String brand) {
        if (brand == null) return Color.parseColor("#607D8B");

        switch (brand.toLowerCase()) {
            case "repsol":
                return Color.parseColor("#EF3340");
            case "cepsa":
            case "moeve":
                return Color.parseColor("#FF6600");
            case "bp":
                return Color.parseColor("#009900");
            case "shell":
                return Color.parseColor("#DD1D21");
            case "galp":
                return Color.parseColor("#FF6B00");
            case "petronor":
                return Color.parseColor("#003087");
            case "carrefour":
                return Color.parseColor("#003CA6");
            case "alcampo":
                return Color.parseColor("#1976D2");
            case "avia":
                return Color.parseColor("#E31837");
            case "ballenoil":
            case "petroprix":
            case "plenergy":
                return Color.parseColor("#455A64");
            default:
                return Color.parseColor("#607D8B");
        }
    }

    public static int getPriceLevelColor(PriceLevel level) {
        if (level == null) return Color.parseColor("#757575");

        switch (level) {
            case CHEAP:
                return Color.parseColor("#388E3C");
            case MID:
                return Color.parseColor("#F57C00");
            case EXPENSIVE:
                return Color.parseColor("#D32F2F");
            case UNKNOWN:
            default:
                return Color.parseColor("#757575");
        }
    }

    public static Bitmap createMarker(Context context,
                                      Gasolinera gasolinera,
                                      FuelType fuelType) {
        String priceText = gasolinera.getFormattedPrice(fuelType);
        String key = gasolinera.getBrandInitial()
                + "|" + gasolinera.getPriceLevel().name()
                + "|" + priceText;

        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap rendered = renderMarker(context, gasolinera, priceText);
        CACHE.put(key, rendered);
        return rendered;
    }

    private static Bitmap renderMarker(Context context,
                                       Gasolinera gasolinera,
                                       String priceText) {
        float density = context.getResources().getDisplayMetrics().density;

        int width = px(density, 74);
        int bubbleHeight = px(density, 56);
        int pinHeight = px(density, 14);
        int height = bubbleHeight + pinHeight;
        int corner = px(density, 10);

        int bgColor = getPriceLevelColor(gasolinera.getPriceLevel());
        String brandAbbr = abbrev(gasolinera.getBrandInitial());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        RectF bubble = new RectF(0, 0, width, bubbleHeight);
        canvas.drawRoundRect(bubble, corner, corner, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(density * 1.8f);
        canvas.drawRoundRect(bubble, corner, corner, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        float pinWidth = px(density, 9);
        Path pin = new Path();
        pin.moveTo(width / 2f - pinWidth, bubbleHeight - density);
        pin.lineTo(width / 2f + pinWidth, bubbleHeight - density);
        pin.lineTo(width / 2f, height);
        pin.close();
        canvas.drawPath(pin, paint);

        float centerX = width / 2f;
        float centerY = bubbleHeight * 0.40f;
        paint.setColor(Color.WHITE);
        paint.setAlpha(215);
        canvas.drawCircle(centerX, centerY, px(density, 13), paint);

        paint.setAlpha(255);
        paint.setColor(bgColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(density * (brandAbbr.length() > 1 ? 10f : 13f));
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        canvas.drawText(brandAbbr, centerX, centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f, paint);

        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(false);
        paint.setTextSize(density * 8.5f);
        canvas.drawText(priceText, centerX, bubbleHeight * 0.82f, paint);

        return bitmap;
    }

    private static int px(float density, int dpValue) {
        return Math.round(density * dpValue);
    }

    private static String abbrev(String value) {
        if (value == null || value.isEmpty()) return "?";
        return value.length() > 2 ? value.substring(0, 2) : value;
    }
}
