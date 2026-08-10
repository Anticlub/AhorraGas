package com.ahorragas.app.detail;

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

import com.ahorragas.app.R;
import com.ahorragas.app.map.MarkerBitmapFactory;
import com.ahorragas.app.model.Discount;
import com.ahorragas.app.model.Electrolinera;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.Vehicle;
import com.ahorragas.app.util.DiscountPrefs;
import com.ahorragas.app.util.FavoritesPrefs;
import com.ahorragas.app.util.VehiclePrefs;

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
    private static final String ARG_IS_ELECTRIC   = "arg_is_electric";
    private static final String ARG_OPERADOR      = "arg_operador";
    private static final String ARG_MAX_POWER_W   = "arg_max_power_w";
    private static final String ARG_ALERT_FUEL    = "arg_alert_fuel";

    /**
     * Crea una nueva instancia del fragment con los datos de la gasolinera.
     *
     * @param gasolinera    Gasolinera cuyos datos se mostrarán.
     * @param alertFuelType Combustible de la alerta que originó esta apertura, o null si no viene de notificación.
     * @return Nueva instancia de GeneralFragment.
     */
    public static GeneralFragment newInstance(Gasolinera gasolinera, FuelType alertFuelType) {
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
        args.putBoolean(ARG_IS_ELECTRIC, gasolinera.isElectric());
        if (gasolinera.getOperador() != null) {
            args.putString(ARG_OPERADOR, gasolinera.getOperador());
            if (gasolinera.isElectric() && gasolinera.getConectores() != null) {
                double maxPowerW = 0;
                for (Electrolinera.Conector c : gasolinera.getConectores()) {
                    if (c.getPotenciaW() != null && c.getPotenciaW() > maxPowerW) {
                        maxPowerW = c.getPotenciaW();
                    }
                }
                if (maxPowerW > 0) {
                    args.putDouble(ARG_MAX_POWER_W, maxPowerW);
                }
            }
        }
        if (alertFuelType != null) {
            args.putString(ARG_ALERT_FUEL, alertFuelType.name());
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

        TextView tvFuelLabel             = view.findViewById(R.id.tvFuelLabel);
        TextView tvPrice                 = view.findViewById(R.id.tvDetailPrice);
        TextView tvDistance              = view.findViewById(R.id.tvDetailDistance);
        TextView tvHorario               = view.findViewById(R.id.tvDetailHorario);
        TextView tvFillCost              = view.findViewById(R.id.tvFillCost);
        TextView tvArrivalCost           = view.findViewById(R.id.tvArrivalCost);
        TextView tvDiscountLabel         = view.findViewById(R.id.tvDiscountLabel);
        TextView tvDiscountPrice         = view.findViewById(R.id.tvDiscountPrice);
        TextView tvDiscountFill          = view.findViewById(R.id.tvDiscountFill);
        View dividerDiscount             = view.findViewById(R.id.dividerDiscount);
        TextView tvFillLabel             = view.findViewById(R.id.tvFillLabel);
        TextView tvArrivalLabel          = view.findViewById(R.id.tvArrivalLabel);
        TextView tvAlertBanner           = view.findViewById(R.id.tvAlertBanner);
        Button btnFavorite               = view.findViewById(R.id.btnFavorite);
        TextView tvFavoritesNotAvailable = view.findViewById(R.id.tvFavoritesNotAvailable);

        // ── Banner de combustible distinto al de la alerta ───────────────────
        String alertFuelName = args.getString(ARG_ALERT_FUEL);
        if (alertFuelName != null) {
            FuelType alertFuel = FuelType.fromString(alertFuelName);
            Double selectedPrice = g.getPrecio(selectedFuel);
            boolean selectedFuelUnavailable = selectedPrice == null || selectedPrice <= 0;
            if (alertFuel != null && alertFuel != selectedFuel && selectedFuelUnavailable) {
                tvAlertBanner.setText(getString(R.string.fmt_alert_fuel_mismatch,
                        alertFuel.displayName()));
                tvAlertBanner.setVisibility(View.VISIBLE);
            } else {
                tvAlertBanner.setVisibility(View.GONE);
            }
        } else {
            tvAlertBanner.setVisibility(View.GONE);
        }

        boolean isElectric = args.getBoolean(ARG_IS_ELECTRIC, false);
        Vehicle activeVehicle = VehiclePrefs.loadActiveVehicle(requireContext());
        Double price = g.getPrecio(selectedFuel);

        tvFuelLabel.setText(selectedFuel.displayName());

        if (isElectric) {
            tvFuelLabel.setText(getString(R.string.label_max_power));
            tvFillLabel.setText(getString(R.string.label_charge_time));
            tvArrivalLabel.setText(getString(R.string.label_energy_cost));
            double maxPwW = args.getDouble(ARG_MAX_POWER_W, 0);
            if (maxPwW > 0) {
                tvPrice.setText(String.format(java.util.Locale.getDefault(), "%.0f kW", maxPwW / 1000.0));
            } else {
                tvPrice.setText(getString(R.string.msg_power_unavailable));
            }

            if (activeVehicle == null || !activeVehicle.isElectric()) {
                tvArrivalCost.setText(getString(R.string.msg_configure_electric_vehicle));
            } else if (!activeVehicle.hasConsumption()) {
                tvArrivalCost.setText(getString(R.string.msg_add_consumption_ev));
            } else if (g.getDistanceMeters() == null || g.getDistanceMeters() <= 0) {
                tvArrivalCost.setText(getString(R.string.msg_distance_unavailable));
            } else {
                double distanceKm = g.getDistanceMeters() / 1000.0;
                Double kwh = activeVehicle.estimateEnergyConsumption(distanceKm);
                tvArrivalCost.setText(String.format(java.util.Locale.getDefault(),
                        getString(R.string.fmt_energy_to_arrive), kwh));
            }

            double maxPowerW = args.getDouble(ARG_MAX_POWER_W, 0);
            double maxPowerKw = maxPowerW / 1000.0;

            if (activeVehicle == null || !activeVehicle.isElectric()) {
                tvFillCost.setText(getString(R.string.msg_configure_electric_vehicle));
            } else if (!activeVehicle.hasTankCapacity() || !activeVehicle.hasChargingPower()) {
                tvFillCost.setText(getString(R.string.msg_add_battery_and_power));
            } else if (maxPowerKw <= 0) {
                tvFillCost.setText(getString(R.string.msg_station_power_unavailable));
            } else {
                Double hours = activeVehicle.estimateChargeTimeHours(maxPowerKw);
                if (hours != null) {
                    int minutes = (int) Math.round(hours * 60);
                    if (minutes < 60) {
                        tvFillCost.setText(String.format(java.util.Locale.getDefault(),
                                getString(R.string.fmt_charge_time_minutes), minutes));
                    } else {
                        int h = minutes / 60;
                        int m = minutes % 60;
                        tvFillCost.setText(String.format(java.util.Locale.getDefault(),
                                getString(R.string.fmt_charge_time_hours_minutes), h, m));
                    }
                } else {
                    tvFillCost.setText(getString(R.string.msg_power_unavailable));
                }
            }

            btnFavorite.setVisibility(View.GONE);
            tvFavoritesNotAvailable.setVisibility(View.VISIBLE);
            tvDiscountLabel.setVisibility(View.GONE);
            tvDiscountPrice.setVisibility(View.GONE);
            tvDiscountFill.setVisibility(View.GONE);
            dividerDiscount.setVisibility(View.GONE);
        } else {
            tvPrice.setText(g.getFormattedPrice(selectedFuel));

            if (activeVehicle == null) {
                tvFillCost.setText(getString(R.string.msg_configure_vehicle_fill));
            } else if (!activeVehicle.hasTankCapacity()) {
                tvFillCost.setText(getString(R.string.msg_add_tank_capacity));
            } else if (price == null || price <= 0) {
                tvFillCost.setText(getString(R.string.msg_price_unavailable));
            } else {
                tvFillCost.setText(String.format(java.util.Locale.getDefault(),
                        "%.2f €", activeVehicle.estimateFillCost(price)));
            }

            if (activeVehicle == null || !activeVehicle.hasConsumption()) {
                tvArrivalCost.setText(getString(R.string.msg_configure_vehicle_cost));
            } else if (g.getDistanceMeters() == null || g.getDistanceMeters() <= 0) {
                tvArrivalCost.setText(getString(R.string.msg_distance_unavailable));
            } else if (price == null || price <= 0) {
                tvArrivalCost.setText(getString(R.string.msg_price_unavailable));
            } else {
                double distanceKm = g.getDistanceMeters() / 1000.0;
                double coste = (distanceKm / 100.0) * activeVehicle.getConsumption() * price;
                tvArrivalCost.setText(String.format(java.util.Locale.getDefault(), "%.2f €", coste));
            }

            List<Discount> discounts = DiscountPrefs.findAllForBrand(requireContext(), g.getMarca());
            if (!discounts.isEmpty() && price != null && price > 0) {
                double discountedPrice = DiscountPrefs.applyAllDiscounts(
                        requireContext(), g.getMarca(), price);

                StringBuilder typeLabel = new StringBuilder();
                for (int i = 0; i < discounts.size(); i++) {
                    if (i > 0) typeLabel.append(" + ");
                    Discount d = discounts.get(i);
                    if (d.getType() == Discount.Type.PERCENTAGE) {
                        typeLabel.append(String.format(java.util.Locale.getDefault(),
                                "%.1f%%", d.getValue()));
                    } else {
                        typeLabel.append(String.format(java.util.Locale.getDefault(),
                                "%.3f €/L", d.getValue() / 100.0));
                    }
                }

                tvDiscountLabel.setText(getString(R.string.fmt_discount_label,
                        discounts.get(0).getBrandName(), typeLabel.toString()));
                tvDiscountPrice.setText(String.format(java.util.Locale.getDefault(),
                        "%.3f €", discountedPrice));

                tvDiscountLabel.setVisibility(View.VISIBLE);
                tvDiscountPrice.setVisibility(View.VISIBLE);
                dividerDiscount.setVisibility(View.VISIBLE);

                if (activeVehicle != null && activeVehicle.hasTankCapacity()) {
                    tvDiscountFill.setText(String.format(java.util.Locale.getDefault(),
                            getString(R.string.fmt_fill_with_discount),
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
        }

        tvDistance.setText(g.getFormattedDistance().isEmpty()
                ? getString(R.string.msg_distance_unavailable)
                : g.getFormattedDistance());
        String horario = g.getFormattedHorario();
        tvHorario.setText(horario != null && !horario.isEmpty() ? horario : getString(R.string.msg_no_available));

        boolean isFav = FavoritesPrefs.isFavorite(requireContext(), g);
        btnFavorite.setText(isFav ? getString(R.string.btn_remove_from_favorites) : getString(R.string.btn_add_to_favorites));

        final Gasolinera finalG = g;
        btnFavorite.setOnClickListener(v -> {
            boolean nowFav = FavoritesPrefs.isFavorite(requireContext(), finalG);
            if (nowFav) {
                FavoritesPrefs.remove(requireContext(), finalG);
                btnFavorite.setText(getString(R.string.btn_add_to_favorites));
            } else {
                FavoritesPrefs.add(requireContext(), finalG);
                btnFavorite.setText(getString(R.string.btn_remove_from_favorites));
            }
            MarkerBitmapFactory.clearCache();
        });
    }
}