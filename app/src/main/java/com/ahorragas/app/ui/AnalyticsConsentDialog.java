package com.ahorragas.app.ui;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.ahorragas.app.R;
import com.ahorragas.app.util.WebLinks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Diálogo de la primera apertura que pide el consentimiento para Firebase
 * Analytics. Aceptar y rechazar tienen el mismo peso, como exige el RGPD, y no
 * se puede cerrar sin elegir. Guardar la decisión es responsabilidad de quien
 * lo invoca.
 */
public final class AnalyticsConsentDialog {

    /** Se invoca cuando el usuario acepta o rechaza. */
    public interface Listener {
        void onDecision(boolean granted);
    }

    private AnalyticsConsentDialog() {}

    public static void show(Activity activity, Listener listener) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.consent_dialog_title)
                .setMessage(R.string.consent_dialog_message)
                .setCancelable(false)
                .setNegativeButton(R.string.consent_dialog_decline,
                        (d, which) -> listener.onDecision(false))
                .setPositiveButton(R.string.consent_dialog_accept,
                        (d, which) -> listener.onDecision(true))
                .setNeutralButton(R.string.pref_privacy_policy, null)
                .create();

        // El botón de la política abre la web sin cerrar el diálogo:
        // el usuario la lee, vuelve y decide.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v ->
                        WebLinks.open(activity, activity.getString(R.string.privacy_policy_url))));
        dialog.show();
    }
}