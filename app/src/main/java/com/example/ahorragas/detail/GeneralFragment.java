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
import com.example.ahorragas.model.Discount;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.DiscountPrefs;
import com.example.ahorragas.util.FavoritesPrefs;
import com.example.ahorragas.util.VehiclePrefs;

import java.util.List;

public class GeneralFragment extends Fragment {

    private static final String ARG_ID            = "arg_id";
    private static final String ARG_MARCA         = "arg_marca";
    private static final String ARG_DIRECCION     = "arg_direccion";
    private static final String ARG_MUNICIPIO     = "arg_municipio";
    private static final String ARG_LAT           = "arg_lat";
    private static final String ARG_LON           = "arg_lon";
    private static final String ARG_HORARIO       = "arg_horario";
    private static final String ARG_PRICES_PREFIX = "arg_price_";
    private static final String ARG_DISTANCE      = "arg_distance";

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

        TextView tvFuelLabel     = view.findViewById(R.id.tvFuelLabel);
        TextView tvPrice         = view.findViewById(R.id.tvDetailPrice);
        TextView tvDistance      = view.findViewById(R.id.tvDetailDistance);
        TextView tvHorario       = view.findViewById(R.id.tvDetailHorario);
        TextView tvFillCost      = view.findViewById(R.id.tvFillCost);
        TextView tvArrivalCost   = view.findViewById(R.id.tvArrivalCost);
        TextView tvDiscountLabel = view.findViewById(R.id.tvDiscountLabel);
        TextView tvDiscountPrice = view.findViewById(R.id.tvDiscountPrice);
        TextView tvDiscountFill  = view.findViewById(R.id.tvDiscountFill);
        View dividerDiscount     = view.findViewById(R.id.dividerDiscount);

        Vehicle activeVehicle = VehiclePrefs.loadActiveVehicle(requireContext());
        Double price = g.getPrecio(selectedFuel);

        tvFuelLabel.setText(selectedFuel.displayName());
        tvPrice.setText(g.getFormattedPrice(selectedFuel));

        // ── Coste de llenado ─────────────────────────────────────────────────
        if (activeVehicle == null) {
            tvFillCost.setText("Configura tu vehículo para calcular el coste de llenado");
        } else if (!activeVehicle.hasTankCapacity()) {
            tvFillCost.setText("Añade la capacidad del depósito en preferencias");
        } else if (price == null || price <= 0) {
            tvFillCost.setText("Precio no disponible");
        } else {
            tvFillCost.setText(String.format(java.util.Locale.getDefault(),
                    "%.2f €", activeVehicle.estimateFillCost(price)));
        }

        // ── Coste de llegada ─────────────────────────────────────────────────
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

        // ── Descuento ────────────────────────────────────────────────────────
        List<Discount> discounts = DiscountPrefs.findAllForBrand(requireContext(), g.getMarca());
        if (!discounts.isEmpty() && price != null && price > 0) {
            double discountedPrice = DiscountPrefs.applyAllDiscounts(
                    requireContext(), g.getMarca(), price);

            // Construir texto legible de los descuentos aplicados
            StringBuilder labelBuilder = new StringBuilder("Descuentos aplicados: ");
            for (int i = 0; i < discounts.size(); i++) {
                Discount d = discounts.get(i);
                if (d.getType() == Discount.Type.PERCENTAGE) {
                    labelBuilder.append(String.format(java.util.Locale.getDefault(),
                            "%.0f%% de descuento", d.getValue()));
                } else {
                    labelBuilder.append(String.format(java.util.Locale.getDefault(),
                            "%.0f cts/L", d.getValue()));
                }
                if (i < discounts.size() - 1) labelBuilder.append(" · ");
            }

            tvDiscountLabel.setText(labelBuilder.toString());
            tvDiscountPrice.setText(String.format(java.util.Locale.getDefault(),
                    "%.3f €", discountedPrice));

            tvDiscountLabel.setVisibility(View.VISIBLE);
            tvDiscountPrice.setVisibility(View.VISIBLE);
            dividerDiscount.setVisibility(View.VISIBLE);

            if (activeVehicle != null && activeVehicle.hasTankCapacity()) {
                tvDiscountFill.setText(String.format(java.util.Locale.getDefault(),
                        "Llenado con descuento: %.2f €",
                        activeVehicle.estimateFillCost(discountedPrice)));
                tvDiscountFill.setVisibility(View.VISIBLE);
            } else {
                tvDiscountFill.setVisibility(View.GONE);
            }
        } else {
            tvDiscountLabel.setVisibility(View.GONE);
            tvDiscountPrice.setVisibility(View.GONE);
            tvDiscountFill.setVisibility(View.GONE);
            dividerDiscount.setVisibility(View.GONE);
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