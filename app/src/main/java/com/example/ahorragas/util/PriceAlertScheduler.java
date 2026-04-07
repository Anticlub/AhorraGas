package com.example.ahorragas.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.ahorragas.PriceAlertWorker;

import java.util.concurrent.TimeUnit;

/**
 * Encapsula el arranque y parada del trabajo periódico de comprobación
 * de alertas de precio.
 */
public final class PriceAlertScheduler {

    private static final String WORK_NAME      = "price_alert_check";
    private static final long   INTERVAL_HOURS = 4;

    private PriceAlertScheduler() {}

    /**
     * Programa la comprobación periódica de alertas si no está ya programada.
     *
     * @param ctx Contexto de la aplicación.
     */
    public static void schedule(Context ctx) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PriceAlertWorker.class,
                INTERVAL_HOURS,
                TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    /**
     * Cancela la comprobación periódica de alertas.
     *
     * @param ctx Contexto de la aplicación.
     */
    public static void cancel(Context ctx) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME);
    }
}