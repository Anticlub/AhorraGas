package com.example.ahorragas.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.ahorragas.R;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.FavoritesPrefs;
import com.example.ahorragas.util.VehiclePrefs;

public class GeneralFragment extends Fragment {

    private static final String ARG_ID             = "arg_id";
    private static final String ARG_MARCA          = "arg_marca";
    private static final String ARG_DIRECCION      = "arg_direccion";
    private static final String ARG_MUNICIPIO      = "arg_municipio";
    private static final String ARG_LAT            = "arg_lat";
    private static final String ARG_LON            = "arg_lon";
    private static final String ARG_HORARIO        = "arg_horario";
    private static final String ARG_PRICES_PREFIX  = "arg_price_";
    private static final String ARG_DISTANCE       = "arg_distance";

    /**
     * Crea una nueva instancia del fragment con los datos de la gasolinera.
     *
     * @param gasolinera Gasolinera cuyos datos se mostrarán.
     * @return Nueva instancia de GeneralFragment.
     */
    public static GeneralFragment newInstance(Gasolinera gasolinera) {
        GeneralFragment fragment = new GeneralFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, gasolinera.getId());
        args.putString(ARG_MARCA, gasolinera.getMarca());
        args.putString(ARG_DIRECCION, gasolinera.getDireccion());
        args.putString(ARG_MUNICIPIO, gasolinera.getMunicipio());
        args.putDouble(ARG_LAT, gasolinera.getLat() != null ? gasolinera.getLat() : 0.0);
        args.putDouble(ARG_LON, gasolinera.getLon() != null ? gasolinera.getLon() : 0.0);
        args.putString(ARG_HORARIO, gasolinera.getHorario());
        for (FuelType fuel : FuelType.values()) {
            Double price = gasolinera.getPrecio(fuel);
            if (price != null) {
                args.putDouble(ARG_PRICES_PREFIX + fuel.name(), price);
            }
        }
        if (gasolinera.getDistanceMeters() != null) {
            args.putDouble(ARG_DISTANCE, gasolinera.getDistanceMeters());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_general, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        FuelType selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name())
        );

        // Reconstruir gasolinera desde args
        Gasolinera g = new Gasolinera(
                args.getInt(ARG_ID),
                args.getString(ARG_MARCA),
                args.getString(ARG_MUNICIPIO),
                args.getString(ARG_DIRECCION),
                args.getDouble(ARG_LAT),
                args.getDouble(ARG_LON),
                null
        );
        g.setHorario(args.getString(ARG_HORARIO));

        for (FuelType fuel : FuelType.values()) {
            String key = ARG_PRICES_PREFIX + fuel.name();
            if (args.containsKey(key)) {
                g.setPrecio(fuel, args.getDouble(key));
            }
        }

        if (args.containsKey(ARG_DISTANCE)) {
            g.setDistanceMeters(args.getDouble(ARG_DISTANCE));
        }

        TextView tvFuelLabel = view.findViewById(R.id.tvFuelLabel);
        TextView tvPrice     = view.findViewById(R.id.tvDetailPrice);
        TextView tvDistance  = view.findViewById(R.id.tvDetailDistance);
        TextView tvHorario   = view.findViewById(R.id.tvDetailHorario);

        tvFuelLabel.setText(selectedFuel.displayName());
        tvPrice.setText(g.getFormattedPrice(selectedFuel));

        Vehicle activeVehicle = VehiclePrefs.loadActiveVehicle(requireContext());
        Double price = g.getPrecio(selectedFuel);

        // ── Coste de llenado ─────────────────────────────────────────────────
        TextView tvFillCost = view.findViewById(R.id.tvFillCost);
        if (activeVehicle == null) {
            tvFillCost.setText("Configura tu vehículo para calcular el coste de llenado");
        } else if (!activeVehicle.hasTankCapacity()) {
            tvFillCost.setText("Añade la capacidad del depósito en preferencias");
        } else if (price == null || price <= 0) {
            tvFillCost.setText("Precio no disponible");
        } else {
            Double fillCost = activeVehicle.estimateFillCost(price);
            tvFillCost.setText(String.format(java.util.Locale.getDefault(), "%.2f €", fillCost));
        }

        // ── Coste de llegada ─────────────────────────────────────────────────
        TextView tvArrivalCost = view.findViewById(R.id.tvArrivalCost);
        if (activeVehicle == null || !activeVehicle.hasConsumption()) {
            tvArrivalCost.setText("Configura tu vehículo para calcular el coste");
        } else if (g.getDistanceMeters() == null || g.getDistanceMeters() <= 0) {
            tvArrivalCost.setText("Distancia no disponible");
        } else if (price == null || price <= 0) {
            tvArrivalCost.setText("Precio no disponible");
        } else {
            double distanceKm = g.getDistanceMeters() / 1000.0;
            double coste = (distanceKm / 100.0) * activeVehicle.getConsumption() * price;
            tvArrivalCost.setText(String.format(java.util.Locale.getDefault(), "%.2f €", coste));
        }

        tvDistance.setText(g.getFormattedDistance().isEmpty()
                ? "Distancia no disponible"
                : g.getFormattedDistance());
        tvHorario.setText(g.getFormattedHorario());

        // ── Botón favorito ───────────────────────────────────────────────────
        Button btnFavorite = view.findViewById(R.id.btnFavorite);
        boolean isFav = FavoritesPrefs.isFavorite(requireContext(), g.getId());
        btnFavorite.setText(isFav ? "❤️ Quitar de favoritos" : "🤍 Añadir a favoritos");

        final Gasolinera finalG = g;
        btnFavorite.setOnClickListener(v -> {
            boolean nowFav = FavoritesPrefs.isFavorite(requireContext(), finalG.getId());
            if (nowFav) {
                FavoritesPrefs.remove(requireContext(), finalG.getId());
                btnFavorite.setText("🤍 Añadir a favoritos");
            } else {
                FavoritesPrefs.add(requireContext(), finalG);
                btnFavorite.setText("❤️ Quitar de favoritos");
            }
        });
    }
}