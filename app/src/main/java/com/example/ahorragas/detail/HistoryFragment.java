package com.example.ahorragas.detail;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ahorragas.R;
import com.example.ahorragas.data.repository.PriceHistoryRepository;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.PriceHistoryEntry;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.VehiclePrefs;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment que muestra el histórico de precios de una estación
 * como gráfico de líneas, filtrado por el combustible del vehículo activo.
 */
public class HistoryFragment extends Fragment {

    private static final String ARG_STATION_ID = "station_id";
    private static final String ARG_STATION_NAME = "station_name";
    private static final int COLOR_ACTIVE_RANGE = 0xFF4DB6AC;
    private static final int COLOR_INACTIVE_RANGE = 0xFF666666;
    private static final int COLOR_LINE = 0xFF4DB6AC;

    private int stationId;
    private String stationName;
    private int currentDays = 7;

    private ProgressBar pbLoading;
    private TextView tvMessage;
    private View layoutContent;
    private TextView tvTitle;
    private LineChart chart;
    private TextView btnRange7, btnRange15, btnRange30;

    private PriceHistoryRepository repository;
    private List<String> chartLabels = new ArrayList<>();

    /**
     * Crea una nueva instancia del fragment.
     *
     * @param stationId   ID de la estación (IDEESS)
     * @param stationName nombre de la estación para el título
     * @return nueva instancia configurada
     */
    public static HistoryFragment newInstance(int stationId, String stationName) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STATION_ID, stationId);
        args.putString(ARG_STATION_NAME, stationName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;
        stationId = args.getInt(ARG_STATION_ID, 0);
        stationName = args.getString(ARG_STATION_NAME, "");

        repository = new PriceHistoryRepository();

        pbLoading = view.findViewById(R.id.pbHistoryLoading);
        tvMessage = view.findViewById(R.id.tvHistoryMessage);
        layoutContent = view.findViewById(R.id.layoutHistoryContent);
        tvTitle = view.findViewById(R.id.tvHistoryTitle);
        chart = view.findViewById(R.id.chartHistory);
        btnRange7 = view.findViewById(R.id.btnRange7);
        btnRange15 = view.findViewById(R.id.btnRange15);
        btnRange30 = view.findViewById(R.id.btnRange30);

        btnRange7.setOnClickListener(v -> loadHistory(7));
        btnRange15.setOnClickListener(v -> loadHistory(15));
        btnRange30.setOnClickListener(v -> loadHistory(30));

        Vehicle active = VehiclePrefs.loadActiveVehicle(requireContext());
        if (active == null || active.getFuelType() == FuelType.ELECTRICO) {
            showMessage("Histórico no disponible para vehículos eléctricos");
            return;
        }

        if (stationId <= 0) {
            showMessage("Histórico no disponible para esta estación");
            return;
        }

        setupChart();
        loadHistory(7);
    }

    /**
     * Carga el histórico de precios para el rango de días indicado.
     *
     * @param days número de días hacia atrás desde hoy
     */
    private void loadHistory(int days) {
        currentDays = days;
        updateRangeButtons();
        showLoading();

        Vehicle active = VehiclePrefs.loadActiveVehicle(requireContext());
        if (active == null) return;
        FuelType fuel = active.getFuelType();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String fechaFin = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -days);
        String fechaInicio = sdf.format(cal.getTime());

        repository.getHistory(stationId, fuel, fechaInicio, fechaFin)
                .observe(getViewLifecycleOwner(), entries -> {
                    if (entries == null || entries.isEmpty()) {
                        showMessage("El histórico de precios no está disponible en este momento");
                        return;
                    }
                    tvTitle.setText(fuel.displayName() + " — " + stationName);
                    renderChart(entries, fuel);
                    showContent();
                });
    }

    /**
     * Configura el aspecto del gráfico.
     */
    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getLegend().setEnabled(false);
        chart.setExtraBottomOffset(8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFF333333);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        YAxis leftAxis = chart.getAxisLeft();
        xAxis.setTextColor(0xFF333333);
        leftAxis.setTextSize(11f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0x1A000000);
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.3f €", value);
            }
        });

        chart.getAxisRight().setEnabled(false);
    }

    /**
     * Renderiza los datos en el gráfico de líneas.
     *
     * @param entries lista de entradas del histórico
     * @param fuel    tipo de combustible (para el color)
     */
    private void renderChart(List<PriceHistoryEntry> entries, FuelType fuel) {
        List<Entry> chartEntries = new ArrayList<>();
        chartLabels = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            PriceHistoryEntry e = entries.get(i);
            chartEntries.add(new Entry(i, (float) e.getPrice()));
            String date = e.getDate();
            chartLabels.add(date.length() >= 10 ? date.substring(5) : date);
        }

        LineDataSet dataSet = new LineDataSet(chartEntries, fuel.displayName());
        dataSet.setColor(COLOR_LINE);
        dataSet.setCircleColor(COLOR_LINE);
        dataSet.setCircleRadius(3f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(COLOR_LINE);
        dataSet.setFillAlpha(30);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(chartLabels));
        chart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                int index = (int) e.getX();
                String date = index < chartLabels.size() ? chartLabels.get(index) : "";
                tvTitle.setText(String.format(java.util.Locale.getDefault(),
                        "%s — %s — %.3f €",
                        fuel.displayName(), date, e.getY()));
            }

            @Override
            public void onNothingSelected() {
                tvTitle.setText(fuel.displayName() + " — " + stationName);
            }
        });
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }

    private void updateRangeButtons() {
        btnRange7.setTextColor(currentDays == 7 ? COLOR_ACTIVE_RANGE : COLOR_INACTIVE_RANGE);
        btnRange15.setTextColor(currentDays == 15 ? COLOR_ACTIVE_RANGE : COLOR_INACTIVE_RANGE);
        btnRange30.setTextColor(currentDays == 30 ? COLOR_ACTIVE_RANGE : COLOR_INACTIVE_RANGE);
    }
    private void showLoading() {
        pbLoading.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showMessage(String message) {
        pbLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        pbLoading.setVisibility(View.GONE);
        tvMessage.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }
}