package com.example.ahorragas.map;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.ahorragas.R;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.NavigationHelper;
import com.example.ahorragas.util.VehiclePrefs;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.util.Locale;

/**
 * InfoWindow personalizado que extiende MarkerInfoWindow (el de osmdroid).
 *
 * MarkerInfoWindow ya rellena automáticamente bubble_title, bubble_description
 * y bubble_subdescription con el título, snippet y subdescripción del Marker,
 * por lo que el aspecto visual es idéntico al popup original.
 *
 * Nosotros solo añadimos:
 *   - Estimación de coste con el vehículo activo
 *   - Botón "Llévame hasta aquí"
 */
public class GasolineraInfoWindow extends MarkerInfoWindow {

    private final Gasolinera gasolinera;
    private final FuelType fuelType;

    public GasolineraInfoWindow(MapView mapView,
                                Gasolinera gasolinera,
                                FuelType fuelType) {
        super(R.layout.marker_info_window, mapView);
        this.gasolinera = gasolinera;
        this.fuelType   = fuelType;
    }

    @Override
    public void onOpen(Object item) {
        // Llama al padre: rellena bubble_title, bubble_description, bubble_subdescription
        super.onOpen(item);

        View v = mView;

        // ── Estimación de coste ──────────────────────────────────────────────
        TextView tvCost = v.findViewById(R.id.iwCostEstimate);
        Vehicle activeVehicle = VehiclePrefs.loadActiveVehicle(v.getContext());
        Double distanceMeters = gasolinera.getDistanceMeters();
        Double price          = gasolinera.getPrecio(fuelType);

        if (activeVehicle != null
                && activeVehicle.hasConsumption()
                && distanceMeters != null && distanceMeters > 0
                && price != null && price > 0) {
            double distKm = distanceMeters / 1000.0;
            Double cost   = activeVehicle.estimateCost(distKm, price);
            if (cost != null) {
                tvCost.setText(String.format(Locale.getDefault(),
                        "⛽ %s · coste est. %.2f €",
                        activeVehicle.getName(), cost));
                tvCost.setVisibility(View.VISIBLE);
            } else {
                tvCost.setVisibility(View.GONE);
            }
        } else {
            tvCost.setVisibility(View.GONE);
        }

        // ── Botón de navegación ──────────────────────────────────────────────
        Button btnNavigate = v.findViewById(R.id.iwBtnNavigate);
        btnNavigate.setOnClickListener(btn ->
                NavigationHelper.navigate(v.getContext(), gasolinera)
        );
    }

    @Override
    public void onClose() {
        // Nada que limpiar
    }
}