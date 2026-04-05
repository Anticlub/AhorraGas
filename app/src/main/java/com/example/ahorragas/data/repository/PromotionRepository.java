package com.example.ahorragas.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ahorragas.model.PromotionPlan;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repositorio que descarga y parsea el CSV de planes de descuento
 * del Geoportal de Gasolineras del Ministerio.
 * Devuelve los datos como LiveData para que el ViewModel los observe.
 */
public class PromotionRepository {

    private static final String CSV_URL =
            "https://geoportalgasolineras.es/downloadReportPlanes?extension=CSV";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 15_000;

    // Índices de columna en el CSV (tras saltarse la fila de metadatos)
    private static final int COL_OPERATOR       = 0;
    private static final int COL_PLAN_NAME      = 1;
    private static final int COL_DESCRIPTION    = 2;
    private static final int COL_VALIDITY       = 3;
    private static final int COL_DISCOUNT_VALUE = 4;
    private static final int COL_DISCOUNT_TYPE  = 5;
    private static final int COL_RECIPIENT      = 6;

    private static final int MIN_COLUMNS = 7;

    private static PromotionRepository instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PromotionRepository() {}

    /**
     * Devuelve la instancia única del repositorio (singleton).
     *
     * @return Instancia de PromotionRepository.
     */
    public static synchronized PromotionRepository getInstance() {
        if (instance == null) {
            instance = new PromotionRepository();
        }
        return instance;
    }

    /**
     * Descarga y parsea el CSV de promociones en un hilo de fondo.
     * Emite una lista vacía si ocurre cualquier error de red o parseo.
     *
     * @return LiveData con la lista de planes de promoción.
     */
    public LiveData<List<PromotionPlan>> fetchPromotions() {
        MutableLiveData<List<PromotionPlan>> liveData = new MutableLiveData<>();

        executor.execute(() -> {
            List<PromotionPlan> plans = new ArrayList<>();
            HttpURLConnection connection = null;

            try {
                connection = openHttpsConnectionTrusted(CSV_URL);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(),
                                Charset.forName("ISO-8859-1")))) {

                    // Fila 0: metadatos ("Fecha: 05/04/2026...") → ignorar
                    // Fila 1: cabeceras reales → ignorar
                    reader.readLine(); // metadatos
                    reader.readLine(); // cabeceras

                    String line;
                    while ((line = reader.readLine()) != null) {
                        PromotionPlan plan = parseLine(line);
                        if (plan != null) plans.add(plan);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("PromotionRepository",
                        "Error descargando promociones: " + e.getMessage(), e);
            } finally {
                if (connection != null) connection.disconnect();
            }

            liveData.postValue(plans);
        });

        return liveData;
    }

    // ─── Helpers privados ────────────────────────────────────────────────────

    private javax.net.ssl.HttpsURLConnection openHttpsConnectionTrusted(String urlString)
            throws Exception {
        javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {}
                }
        };

        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new java.security.SecureRandom());

        javax.net.ssl.HttpsURLConnection conn =
                (javax.net.ssl.HttpsURLConnection) new URL(urlString).openConnection();
        conn.setSSLSocketFactory(sslContext.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        return conn;
    }

    /**
     * Parsea una línea CSV respetando campos entre comillas que contienen comas.
     *
     * @param line Línea cruda del CSV.
     * @return PromotionPlan o null si la línea no tiene el formato esperado.
     */
    private PromotionPlan parseLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;

        List<String> fields = splitCsvLine(line);
        if (fields.size() < MIN_COLUMNS) return null;

        String operator     = fields.get(COL_OPERATOR);
        String planName     = fields.get(COL_PLAN_NAME);
        String description  = fields.get(COL_DESCRIPTION);
        String validity     = fields.get(COL_VALIDITY);
        double value        = parseDouble(fields.get(COL_DISCOUNT_VALUE));
        PromotionPlan.DiscountType type = parseDiscountType(fields.get(COL_DISCOUNT_TYPE));
        String recipient    = fields.get(COL_RECIPIENT);

        return new PromotionPlan(operator, planName, description,
                validity, value, type, recipient);
    }

    /**
     * Divide una línea CSV teniendo en cuenta campos entre comillas dobles.
     *
     * @param line Línea CSV cruda.
     * @return Lista de campos ya sin comillas envolventes.
     */
    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Comilla escapada ("") dentro de un campo entrecomillado
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private double parseDouble(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private PromotionPlan.DiscountType parseDiscountType(String raw) {
        if (raw == null) return PromotionPlan.DiscountType.OTHER;
        String lower = raw.toLowerCase();
        if (lower.contains("céntimo") || lower.contains("centimo")) {
            return PromotionPlan.DiscountType.CENTS_PER_LITER;
        } else if (lower.contains("porcentaje") || lower.contains("%")) {
            return PromotionPlan.DiscountType.PERCENTAGE;
        }
        return PromotionPlan.DiscountType.OTHER;
    }
}