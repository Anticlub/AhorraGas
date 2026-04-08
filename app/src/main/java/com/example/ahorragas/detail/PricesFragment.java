package com.example.ahorragas.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ahorragas.R;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;

public class PricesFragment extends Fragment {

    private static final String ARG_ID        = "arg_id";
    private static final String ARG_MARCA     = "arg_marca";
    private static final String ARG_DIRECCION = "arg_direccion";
    private static final String ARG_MUNICIPIO = "arg_municipio";
    private static final String ARG_LAT       = "arg_lat";
    private static final String ARG_LON       = "arg_lon";
    private static final String ARG_HORARIO   = "arg_horario";
    private static final String ARG_PRICES_PREFIX = "arg_price_";
    private static final String ARG_IS_ELECTRIC    = "arg_is_electric";
    private static final String ARG_CONECTORES     = "arg_conectores";

    /**
     * Crea una nueva instancia del fragment con los datos de la gasolinera.
     *
     * @param gasolinera Gasolinera cuyos precios se mostrarán.
     * @return Nueva instancia de PricesFragment.
     */
    public static PricesFragment newInstance(Gasolinera gasolinera) {
        PricesFragment fragment = new PricesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, gasolinera.getId());
        args.putString(ARG_MARCA, gasolinera.getMarca());
        args.putString(ARG_DIRECCION, gasolinera.getDireccion());
        args.putString(ARG_MUNICIPIO, gasolinera.getMunicipio());
        args.putDouble(ARG_LAT, gasolinera.getLat() != null ? gasolinera.getLat() : 0.0);
        args.putDouble(ARG_LON, gasolinera.getLon() != null ? gasolinera.getLon() : 0.0);
        args.putString(ARG_HORARIO, gasolinera.getHorario());
        for (com.example.ahorragas.model.FuelType fuel : com.example.ahorragas.model.FuelType.values()) {
            Double price = gasolinera.getPrecio(fuel);
            if (price != null) {
                args.putDouble(ARG_PRICES_PREFIX + fuel.name(), price);
            }
        }
        args.putBoolean(ARG_IS_ELECTRIC, gasolinera.isElectric());
        if (gasolinera.isElectric() && gasolinera.getConectores() != null) {
            // Serializamos los conectores como string: "CCS2|mode4DC|350000.0;Tipo2|mode3|22000.0"
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < gasolinera.getConectores().size(); i++) {
                com.example.ahorragas.model.Electrolinera.Conector c =
                        gasolinera.getConectores().get(i);
                if (i > 0) sb.append(";");
                sb.append(c.getTipo() != null ? c.getTipo().name() : "UNKNOWN");
                sb.append("|");
                sb.append(c.getModoRecarga() != null ? c.getModoRecarga() : "");
                sb.append("|");
                sb.append(c.getPotenciaW() != null ? c.getPotenciaW() : "0");
            }
            args.putString(ARG_CONECTORES, sb.toString());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

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

        for (com.example.ahorragas.model.FuelType fuel : com.example.ahorragas.model.FuelType.values()) {
            String key = ARG_PRICES_PREFIX + fuel.name();
            if (args.containsKey(key)) {
                g.setPrecio(fuel, args.getDouble(key));
            }
        }

        LinearLayout container2 = view.findViewById(R.id.pricesContainer);

        boolean isElectric = args.getBoolean(ARG_IS_ELECTRIC, false);

        if (isElectric) {
            String conectoresRaw = args.getString(ARG_CONECTORES, "");
            if (conectoresRaw.isEmpty()) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("Sin datos de conectores");
                tvEmpty.setTextColor(0xFF757575);
                tvEmpty.setTextSize(14);
                container2.addView(tvEmpty);
            } else {
                String[] items = conectoresRaw.split(";");
                for (String item : items) {
                    String[] parts = item.split("\\|", -1);
                    if (parts.length < 3) continue;

                    com.example.ahorragas.model.ConnectorType tipo =
                            com.example.ahorragas.model.ConnectorType.valueOf(parts[0]);
                    String modo = parts[1];
                    double potenciaW = 0;
                    try { potenciaW = Double.parseDouble(parts[2]); }
                    catch (NumberFormatException ignored) {}

                    String tipoRecarga = modo.contains("DC") ? "Rápida (DC)" : "Normal (AC)";
                    String potenciaStr = potenciaW > 0
                            ? String.format(java.util.Locale.getDefault(),
                            "%.0f kW", potenciaW / 1000.0)
                            : "N/D";

                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, dp(8), 0, dp(8));

                    TextView tvConector = new TextView(requireContext());
                    tvConector.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    tvConector.setText(tipo.getDisplayName() + " · " + tipoRecarga);
                    tvConector.setTextColor(0xFF212121);
                    tvConector.setTextSize(14);

                    TextView tvPotencia = new TextView(requireContext());
                    tvPotencia.setText(potenciaStr);
                    tvPotencia.setTextColor(0xFF1976D2);
                    tvPotencia.setTextSize(14);
                    tvPotencia.setTypeface(null, android.graphics.Typeface.BOLD);

                    row.addView(tvConector);
                    row.addView(tvPotencia);
                    container2.addView(row);

                    View divider = new View(requireContext());
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(0xFFE0E0E0);
                    container2.addView(divider);
                }
            }
        } else {
            for (FuelType fuel : FuelType.values()) {
                String price = g.getFormattedPrice(fuel);
                if (price.equals("N/D")) continue;

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(8), 0, dp(8));

                TextView tvFuel = new TextView(requireContext());
                tvFuel.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvFuel.setText(fuel.displayName());
                tvFuel.setTextColor(0xFF212121);
                tvFuel.setTextSize(14);

                TextView tvPrice = new TextView(requireContext());
                tvPrice.setText(price);
                tvPrice.setTextColor(0xFF388E3C);
                tvPrice.setTextSize(14);
                tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);

                row.addView(tvFuel);
                row.addView(tvPrice);
                container2.addView(row);

                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(0xFFE0E0E0);
                container2.addView(divider);
            }
        }    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}