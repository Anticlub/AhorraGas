package com.example.ahorragas;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ahorragas.data.RepoError;
import com.example.ahorragas.data.RoomGasolineraDataSource;
import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceAlert;
import com.example.ahorragas.util.PriceAlertPrefs;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PriceAlertWorker extends Worker {

    private static final String CHANNEL_ID      = "price_alerts";
    private static final long   MIN_INTERVAL_MS =TimeUnit.HOURS.toMillis(24);

    public PriceAlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        List<PriceAlert> alerts = PriceAlertPrefs.loadAll(ctx);
        if (alerts.isEmpty()) return Result.success();

        createNotificationChannel(ctx);

        AppDatabase db = AppDatabase.getInstance(ctx);
        RoomGasolineraDataSource roomDs = new RoomGasolineraDataSource(db);

        List<Gasolinera> gasolineras;
        try {
            gasolineras = roomDs.loadGasolineras();
        } catch (RepoError e) {
            android.util.Log.e("PriceAlertWorker", "Error cargando gasolineras: " + e.getMessage());
            return Result.retry();
        }

        for (PriceAlert alert : alerts) {
            try {
                checkAlert(ctx, gasolineras, alert);
            } catch (Exception e) {
                android.util.Log.e("PriceAlertWorker",
                        "Error comprobando alerta " + alert.getKey(), e);
            }
        }

        return Result.success();
    }

    // ─── PRIVADO ──────────────────────────────────────────────────────────────

    private void checkAlert(Context ctx, List<Gasolinera> gasolineras, PriceAlert alert) {
        Gasolinera target = null;
        for (Gasolinera g : gasolineras) {
            if (g.getId() == alert.getGasolineraId()) {
                target = g;
                break;
            }
        }
        if (target == null) return;

        Double currentPrice = target.getPrecio(alert.getFuelType());
        if (currentPrice == null) return;

        boolean priceBelowThreshold = currentPrice <= alert.getTargetPrice();
        boolean cooldownPassed = (System.currentTimeMillis() - alert.getLastNotifiedAt())
                >= MIN_INTERVAL_MS;

        if (priceBelowThreshold && cooldownPassed) {
            sendNotification(ctx, alert, currentPrice);
            PriceAlertPrefs.updateLastNotified(ctx, alert.getKey(), System.currentTimeMillis());
        }
    }

    private void sendNotification(Context ctx, PriceAlert alert, double currentPrice) {
        NotificationManager manager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Uri soundUri = Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.burbuja);

        String title = "🔔 " + alert.getGasolineraName();
        String body  = String.format(
                java.util.Locale.getDefault(),
                "%s a %.3f €/L (tu alerta: %.3f €/L)",
                alert.getFuelType().displayName(),
                currentPrice,
                alert.getTargetPrice()
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(alert.getKey().hashCode(), builder.build());
    }

    private void createNotificationChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.burbuja);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de precio",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notificaciones cuando el precio baja del umbral configurado");
            channel.setSound(soundUri, audioAttributes);

            NotificationManager manager =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}