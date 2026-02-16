package com.example.ahorragas;

import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ahorragas.location.LocationHelper;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.util.GasolineraSorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TestDistanceActivity extends AppCompatActivity {

    private TextView tvOut;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_distance);

        tvOut = findViewById(R.id.tvOut);
        Button btnRun = findViewById(R.id.btnRun);
        Button btnTop5 = findViewById(R.id.btnTop5);
        Button btn2km = findViewById(R.id.btn2km);
        Button btn5kmMax10 = findViewById(R.id.btn5kmMax10);

        List<Gasolinera> gasolineras = createAlcalaMock();

        btnTop5.setOnClickListener(v ->
                testTop(gasolineras, 5)
        );

        btn2km.setOnClickListener(v ->
                testRadius(gasolineras, 20000)
        );

        btn5kmMax10.setOnClickListener(v ->
                testRadiusLimit(gasolineras, 50000, 10)
        );

        locationHelper = new LocationHelper(this);

        btnRun.setOnClickListener(v -> runTest());
    }
    private void testTop(List<Gasolinera> data, int n) {
        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                List<Gasolinera> result =
                        GasolineraSorter.getTopClosest(data,
                                location.getLatitude(),
                                location.getLongitude(),
                                n);

                showResult("Top " + n, result);
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                tvOut.setText("Error ubicación: " + error.name());
            }
        });
    }

    private void testRadius(List<Gasolinera> data, double meters) {
        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                List<Gasolinera> result =
                        GasolineraSorter.getWithinRadius(data,
                                location.getLatitude(),
                                location.getLongitude(),
                                meters);

                showResult("Dentro de " + (meters/1000) + " km", result);
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                tvOut.setText("Error ubicación: " + error.name());
            }
        });
    }

    private void testRadiusLimit(List<Gasolinera> data, double meters, int max) {
        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                List<Gasolinera> result =
                        GasolineraSorter.getWithinRadius(data,
                                location.getLatitude(),
                                location.getLongitude(),
                                meters,
                                max);

                showResult("Dentro de " + (meters/1000) + " km (máx " + max + ")", result);
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                tvOut.setText("Error ubicación: " + error.name());
            }
        });
    }

    private void showResult(String title, List<Gasolinera> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\nTotal: ").append(list.size()).append("\n\n");

        for (Gasolinera g : list) {
            sb.append(g.getMarca())
                    .append(" | ")
                    .append(String.format("%.0f m", g.getDistanceMeters()))
                    .append("\n");
        }

        tvOut.setText(sb.toString());
    }

    private void runTest() {
        tvOut.setText("Obteniendo ubicación…");

        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                double userLat = location.getLatitude();
                double userLon = location.getLongitude();

                List<Gasolinera> data = createMockGasolineras();

                // Prueba: top 5 más cercanas
                List<Gasolinera> top = GasolineraSorter.getTopClosest(data, userLat, userLon, 5);

                StringBuilder sb = new StringBuilder();
                sb.append("Usuario: ")
                        .append(userLat).append(", ").append(userLon)
                        .append("\n\nTop 5 cercanas:\n");

                for (Gasolinera g : top) {
                    sb.append("- ")
                            .append(g.getMarca()).append(" | ")
                            .append(g.getMunicipio()).append(" | ")
                            .append(formatMeters(g.getDistanceMeters()))
                            .append("\n");
                }

                tvOut.setText(sb.toString());
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                tvOut.setText("Error ubicación: " + error.name());
            }
        });
    }

    private String formatMeters(Double meters) {
        if (meters == null) return "—";
        if (meters >= 1000) {
            return String.format(Locale.getDefault(), "%.2f km", meters / 1000.0);
        }
        return String.format(Locale.getDefault(), "%.0f m", meters);
    }

    private List<Gasolinera> createMockGasolineras() {
        List<Gasolinera> list = new ArrayList<>();

        // Algunas válidas (Madrid y alrededores) - ajusta si quieres
        list.add(new Gasolinera(1, "Repsol", "Madrid", "Centro", 40.4168, -3.7038, 1.60));
        list.add(new Gasolinera(2, "Cepsa", "Getafe", "Calle B", 40.3083, -3.7327, 1.55));
        list.add(new Gasolinera(3, "BP", "Alcalá", "Calle C", 40.4819, -3.3641, 1.58));
        list.add(new Gasolinera(4, "Shell", "Leganés", "Calle D", 40.3272, -3.7635, 1.57));

        // Inválidas para probar filtro
        list.add(new Gasolinera(999, "INVÁLIDA 0,0", "—", "—", 0.0, 0.0, 9.99));
        list.add(new Gasolinera(998, "INVÁLIDA lat", "—", "—", 200.0, 10.0, 9.99));
        list.add(new Gasolinera(997, "INVÁLIDA null", "—", "—", null, null, 9.99));

        return list;
    }

    private List<Gasolinera> createAlcalaMock() {
        List<Gasolinera> list = new ArrayList<>();

        list.add(new Gasolinera(14075, "GASEXPRESS", "Alcalá de Henares", "CARRETERA DAGANZO KM. 3", 40.492861, -3.381167, 1.239));
        list.add(new Gasolinera(13592, "AVIA", "Alcalá de Henares", "CALLE MEJICO, 17", 40.498250, -3.390139, 1.339));
        list.add(new Gasolinera(15959, "LAVAPLUS", "Alcalá de Henares", "CARRERA DE AJALVIR, 3", 40.490167, -3.383806, 1.239));
        list.add(new Gasolinera(14833, "ENERGY", "Alcalá de Henares", "CALLE PERU, 31", 40.508333, -3.396917, 1.339));
        list.add(new Gasolinera(12721, "GALP", "Alcalá de Henares", "AVDA MADRID ESQ CARLOS III", 40.476667, -3.393972, 1.299));
        list.add(new Gasolinera(4697, "GALP", "Alcalá de Henares", "N-II km 29", 40.493083, -3.386583, 1.349));
        list.add(new Gasolinera(4698, "GALP", "Alcalá de Henares", "N-II km 29 margen I", 40.494167, -3.388028, 1.349));
        list.add(new Gasolinera(4716, "SHELL", "Alcalá de Henares", "AVDA PUERTA DE MADRID, 20", 40.480750, -3.374889, 1.389));
        list.add(new Gasolinera(14083, "PLENERGY", "Alcalá de Henares", "AVDA MADRID, 60", 40.474556, -3.401000, 1.239));
        list.add(new Gasolinera(3103, "BP", "Alcalá de Henares", "M-300 KM 26,4", 40.472639, -3.408000, 1.409));
        list.add(new Gasolinera(13965, "SHELL", "Alcalá de Henares", "VIA COMPLUTENSE, 105", 40.490556, -3.354139, 1.309));
        list.add(new Gasolinera(15797, "BALLENOIL", "Alcalá de Henares", "CALLE VARSOVIA, 2", 40.497694, -3.341111, 1.239));
        list.add(new Gasolinera(4588, "REPSOL", "Alcalá de Henares", "VIA COMPLUTENSE, 41", 40.486278, -3.362250, 1.509));
        list.add(new Gasolinera(4490, "REPSOL", "Alcalá de Henares", "AVDA GUADALAJARA, 29", 40.487472, -3.358111, 1.509));
        list.add(new Gasolinera(15009, "MOEVE", "Alcalá de Henares", "VIA COMPLUTENSE, 165", 40.497167, -3.343306, 1.479));
        list.add(new Gasolinera(2929, "GALP", "Alcalá de Henares", "VIA COMPLUTENSE ESQ AVILA", 40.493750, -3.348111, 1.359));
        list.add(new Gasolinera(3102, "BP", "Alcalá de Henares", "ANTIGUA N-II KM 26", 40.470556, -3.412444, 1.429));
        list.add(new Gasolinera(3175, "REPSOL", "Alcalá de Henares", "M-300 KM 30.3", 40.471222, -3.413139, 1.489));
        list.add(new Gasolinera(3079, "ALCAMPO", "Alcalá de Henares", "C.C. LA DEHESA KM 34", 40.505111, -3.328833, 1.249));
        list.add(new Gasolinera(3083, "CARREFOUR", "Alcalá de Henares", "POLIGONO ESPARTALES", 40.503778, -3.369444, 1.449));
        list.add(new Gasolinera(13308, "REPSOL", "Alcalá de Henares", "AVDA JUAN CARLOS I", 40.481167, -3.400361, 1.489));
        list.add(new Gasolinera(3145, "BP", "Alcalá de Henares", "AVDA JUAN CARLOS I, 44", 40.482167, -3.396750, 1.494));
        list.add(new Gasolinera(13923, "PETROPRIX", "Alcalá de Henares", "CALLE ISAAC NEWTON, 155", 40.494139, -3.390917, 1.239));
        list.add(new Gasolinera(8442, "SHELL", "Alcalá de Henares", "CARRETERA AJARVIL", 40.490361, -3.384778, 1.379));
        list.add(new Gasolinera(15069, "SUPECO", "Alcalá de Henares", "POLIGONO LA GARENA", 40.489917, -3.381806, 1.249));
        list.add(new Gasolinera(3067, "GALP", "Alcalá de Henares", "CALLE VILLAMALEA, 2", 40.507750, -3.352722, 1.349));
        list.add(new Gasolinera(3153, "BALLENOIL", "Alcalá de Henares", "M-300 KM 26,9", 40.474000, -3.403306, 1.239));
        list.add(new Gasolinera(14679, "BALLENOIL", "Alcalá de Henares", "CARRETERA DAGANZO KM 5", 40.492889, -3.381556, 1.239));
        list.add(new Gasolinera(4325, "MOEVE", "Alcalá de Henares", "AVDA DAGANZO, 12", 40.492361, -3.379222, 1.479));

        return list;
    }
}