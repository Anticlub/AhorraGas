package com.example.ahorragas.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.BuildConfig;
import com.example.ahorragas.R;
import com.example.ahorragas.model.Gasolinera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

public class ReviewsFragment extends Fragment {

    private static final String ARG_LAT       = "arg_lat";
    private static final String ARG_LON       = "arg_lon";
    private static final String ARG_NOMBRE    = "arg_nombre";
    private static final String ARG_ELECTRICA = "arg_electrica";

    private TextView tvStatus;
    private TextView tvRatingNumber;
    private TextView tvRatingStars;
    private TextView tvRatingCount;
    private View layoutRatingHeader;
    private RecyclerView rvReviews;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Crea una nueva instancia del fragment con los datos de la gasolinera.
     *
     * @param gasolinera Gasolinera cuyos datos se mostrarán.
     * @return Nueva instancia de ReviewsFragment.
     */
    public static ReviewsFragment newInstance(Gasolinera gasolinera) {
        ReviewsFragment fragment = new ReviewsFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, gasolinera.getLat() != null ? gasolinera.getLat() : 0.0);
        args.putDouble(ARG_LON, gasolinera.getLon() != null ? gasolinera.getLon() : 0.0);
        args.putString(ARG_NOMBRE, gasolinera.getMarca() + " " + gasolinera.getMunicipio());
        args.putBoolean(ARG_ELECTRICA, gasolinera.isElectric());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatus           = view.findViewById(R.id.tvReviewsStatus);
        tvRatingNumber     = view.findViewById(R.id.tvRatingNumber);
        tvRatingStars      = view.findViewById(R.id.tvRatingStars);
        tvRatingCount      = view.findViewById(R.id.tvRatingCount);
        layoutRatingHeader = view.findViewById(R.id.layoutRatingHeader);
        rvReviews          = view.findViewById(R.id.rvReviews);

        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle args = getArguments();
        if (args == null) return;
        double lat        = args.getDouble(ARG_LAT);
        double lon        = args.getDouble(ARG_LON);
        String nombre     = args.getString(ARG_NOMBRE, "");
        boolean electrica = args.getBoolean(ARG_ELECTRICA, false);
        loadReviews(lat, lon, nombre, electrica);
    }

    /**
     * Busca la estación en Places API (New) por coordenadas y carga sus reseñas.
     *
     * @param lat       Latitud de la estación.
     * @param lon       Longitud de la estación.
     * @param nombre    Nombre aproximado para búsqueda.
     * @param electrica true si es una electrolinera.
     */
    private void loadReviews(double lat, double lon, String nombre, boolean electrica) {
        layoutRatingHeader.setVisibility(View.GONE);
        rvReviews.setVisibility(View.GONE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Cargando opiniones...");

        executor.execute(() -> {
            try {
                String placeId = fetchPlaceId(lat, lon, electrica);
                if (placeId == null) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        tvStatus.setText("No se encontraron opiniones para esta estación.");
                    });
                    return;
                }

                JSONObject details = fetchPlaceDetails(placeId);
                if (details == null) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        tvStatus.setText("No se pudieron cargar las opiniones.");
                    });
                    return;
                }

                double rating    = details.optDouble("rating", 0);
                int totalRatings = details.optInt("userRatingCount", 0);
                JSONArray reviews = details.optJSONArray("reviews");

                List<ReviewItem> items = new ArrayList<>();
                if (reviews != null) {
                    for (int i = 0; i < reviews.length(); i++) {
                        JSONObject r  = reviews.getJSONObject(i);
                        String author = "Anónimo";
                        JSONObject authorObj = r.optJSONObject("authorAttribution");
                        if (authorObj != null) {
                            author = authorObj.optString("displayName", "Anónimo");
                        }
                        int stars  = r.optInt("rating", 0);
                        String text = "";
                        JSONObject textObj = r.optJSONObject("text");
                        if (textObj != null) {
                            text = textObj.optString("text", "");
                        }
                        String publishTime = r.optString("publishTime", "");
                        String date = "--";
                        if (!publishTime.isEmpty()) {
                            try {
                                SimpleDateFormat isoFormat = new SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                String cleanTime = publishTime.replaceAll("\\.\\d+Z$", "Z")
                                        .replace("Z", "");
                                Date parsedDate = isoFormat.parse(cleanTime);
                                if (parsedDate != null) {
                                    date = new SimpleDateFormat("dd/MM/yyyy",
                                            Locale.getDefault()).format(parsedDate);
                                }
                            } catch (Exception ignored) {}
                        }
                        if (!text.isEmpty()) {
                            items.add(new ReviewItem(author, stars, text, date));
                        }
                    }
                }

                if (electrica) {
                    sortReviewsForElectric(items);
                }

                final double finalRating      = rating;
                final int finalTotal          = totalRatings;
                final List<ReviewItem> finalItems = items;

                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    if (finalRating > 0) {
                        tvRatingNumber.setText(String.format(Locale.getDefault(),
                                "%.1f", finalRating));
                        tvRatingStars.setText(starsText(finalRating));
                        tvRatingCount.setText(finalTotal + " opiniones en Google Maps");
                        layoutRatingHeader.setVisibility(View.VISIBLE);
                    }

                    if (finalItems.isEmpty()) {
                        tvStatus.setText("No hay reseñas escritas para esta estación.");
                    } else {
                        tvStatus.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                        rvReviews.setAdapter(new ReviewAdapter(finalItems));
                    }
                });

            } catch (QuotaExceededException e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    tvStatus.setText("😕 Ahora mismo no podemos mostrarte opiniones. Disculpa las molestias, inténtalo más tarde.");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    tvStatus.setText("No se pudieron cargar las opiniones.");
                });
            }
        });
    }

    /**
     * Busca el place_id más cercano usando Text Search (New).
     *
     * @param lat       Latitud.
     * @param lon       Longitud.
     * @param electrica true si es una electrolinera.
     * @return place_id del lugar más cercano o null si no se encuentra.
     */
    private String fetchPlaceId(double lat, double lon, boolean electrica) throws Exception {
        String urlStr = "https://places.googleapis.com/v1/places:searchText";

        String query = electrica ? "punto de carga electrica" : "gasolinera";

        JSONObject body = new JSONObject();
        body.put("textQuery", query);
        body.put("maxResultCount", 5);
        JSONObject locationBias = new JSONObject();
        JSONObject circle = new JSONObject();
        JSONObject center = new JSONObject();
        center.put("latitude", lat);
        center.put("longitude", lon);
        circle.put("center", center);
        circle.put("radius", 100.0);
        locationBias.put("circle", circle);
        body.put("locationBias", locationBias);

        JSONObject response = httpPost(urlStr, body, "places.id,places.location,places.name");
        if (response == null) return null;

        checkQuotaNew(response);

        JSONArray places = response.optJSONArray("places");
        if (places == null || places.length() == 0) return null;

        String bestPlaceId  = null;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < places.length(); i++) {
            JSONObject place    = places.getJSONObject(i);
            JSONObject location = place.optJSONObject("location");
            if (location == null) continue;

            double resultLat = location.optDouble("latitude", 0);
            double resultLng = location.optDouble("longitude", 0);

            double dLat = resultLat - lat;
            double dLon = resultLng - lon;
            double dist = Math.sqrt(dLat * dLat + dLon * dLon);

            if (dist < bestDistance) {
                bestDistance = dist;
                String name = place.optString("name", null);
                if (name != null && name.startsWith("places/")) {
                    bestPlaceId = name.substring("places/".length());
                }
            }
        }

        return bestPlaceId;
    }

    /**
     * Obtiene el rating y las reseñas usando Place Details (New).
     *
     * @param placeId Identificador del lugar en Google Places.
     * @return JSONObject con los detalles o null si falla.
     */
    private JSONObject fetchPlaceDetails(String placeId) throws Exception {
        String urlStr = "https://places.googleapis.com/v1/places/" + placeId
                + "?languageCode=es";

        JSONObject response = httpGet(urlStr, "rating,userRatingCount,reviews");
        if (response == null) return null;

        checkQuotaNew(response);

        return response;
    }

    /**
     * Reordena las reseñas poniendo primero las que mencionan
     * el estado del punto de carga, relevantes para electrolineras.
     *
     * @param items Lista de reseñas a reordenar.
     */
    private void sortReviewsForElectric(List<ReviewItem> items) {
        String[] keywords = {
                "roto", "averiado", "no funciona", "fuera de servicio",
                "estropeado", "ocupado", "lento", "funciona", "operativo",
                "cargando", "cargador", "error", "fallo", "problema",
                "no carga", "desconectado", "bloqueado", "libre", "disponible"
        };

        items.sort((a, b) -> {
            boolean aRelevant = containsKeyword(a.text.toLowerCase(), keywords);
            boolean bRelevant = containsKeyword(b.text.toLowerCase(), keywords);
            if (aRelevant && !bRelevant) return -1;
            if (!aRelevant && bRelevant) return 1;
            return 0;
        });
    }

    /**
     * Comprueba si un texto contiene alguna de las palabras clave dadas.
     *
     * @param text     Texto a comprobar.
     * @param keywords Palabras clave a buscar.
     * @return true si contiene alguna.
     */
    private boolean containsKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Realiza una petición HTTP POST con body JSON y FieldMask.
     *
     * @param urlStr    URL a consultar.
     * @param body      Cuerpo de la petición.
     * @param fieldMask Campos a solicitar.
     * @return JSONObject con la respuesta o null si falla.
     */
    private JSONObject httpPost(String urlStr, JSONObject body,
                                String fieldMask) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Goog-Api-Key", BuildConfig.PLACES_API_KEY);
        conn.setRequestProperty("X-Goog-FieldMask", fieldMask);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.close();

        InputStream is = conn.getInputStream();
        String response = new java.util.Scanner(is).useDelimiter("\\A").next();
        conn.disconnect();
        return new JSONObject(response);
    }

    /**
     * Realiza una petición HTTP GET con FieldMask para Places API (New).
     *
     * @param urlStr    URL a consultar.
     * @param fieldMask Campos a solicitar.
     * @return JSONObject con la respuesta o null si falla.
     */
    private JSONObject httpGet(String urlStr, String fieldMask) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Goog-Api-Key", BuildConfig.PLACES_API_KEY);
        conn.setRequestProperty("X-Goog-FieldMask", fieldMask);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        InputStream is = conn.getInputStream();
        String response = new java.util.Scanner(is).useDelimiter("\\A").next();
        conn.disconnect();
        return new JSONObject(response);
    }

    /**
     * Comprueba si la respuesta de la API New indica error de cuota.
     *
     * @param response JSONObject de la respuesta.
     */
    private void checkQuotaNew(JSONObject response) throws QuotaExceededException {
        JSONObject error = response.optJSONObject("error");
        if (error != null) {
            int code = error.optInt("code", 0);
            if (code == 429 || code == 403) {
                throw new QuotaExceededException();
            }
        }
    }

    /**
     * Convierte un rating numérico en estrellas emoji.
     *
     * @param rating Rating entre 0 y 5.
     * @return String con estrellas.
     */
    private String starsText(double rating) {
        int full  = (int) rating;
        int empty = 5 - full;
        return "★".repeat(full) + "☆".repeat(empty);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ─── Excepción de cuota ───────────────────────────────────────────────────

    private static class QuotaExceededException extends Exception {}

    // ─── Modelo interno ───────────────────────────────────────────────────────

    static class ReviewItem {
        final String author;
        final int stars;
        final String text;
        final String date;

        ReviewItem(String author, int stars, String text, String date) {
            this.author = author;
            this.stars  = stars;
            this.text   = text;
            this.date   = date;
        }
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────

    static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

        private final List<ReviewItem> items;

        ReviewAdapter(List<ReviewItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvAuthor;
            final TextView tvStars;
            final TextView tvDate;
            final TextView tvText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAuthor = itemView.findViewById(R.id.tvReviewAuthor);
                tvStars  = itemView.findViewById(R.id.tvReviewStars);
                tvDate   = itemView.findViewById(R.id.tvReviewDate);
                tvText   = itemView.findViewById(R.id.tvReviewText);
            }

            void bind(ReviewItem item) {
                tvAuthor.setText(item.author);
                tvStars.setText("★".repeat(item.stars) + "☆".repeat(5 - item.stars));
                tvDate.setText(item.date);
                tvText.setText(item.text);
            }
        }
    }
}