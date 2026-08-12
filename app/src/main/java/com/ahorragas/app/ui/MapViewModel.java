package com.ahorragas.app.ui;

import android.app.Application;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.ahorragas.app.R;
import com.ahorragas.app.data.ElectrolineraRepository;
import com.ahorragas.app.data.EstacionRepository;
import com.ahorragas.app.data.GasolineraRepository;
import com.ahorragas.app.data.RepoError;
import com.ahorragas.app.data.RoomElectrolineraDataSource;
import com.ahorragas.app.data.RoomGasolineraDataSource;
import com.ahorragas.app.data.local.AppDatabase;
import com.ahorragas.app.data.remote.RemoteDgtDataSource;
import com.ahorragas.app.data.repository.GeocodingRepository;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.util.GasolineraSorter;
import com.ahorragas.app.util.GeoUtils;
import com.ahorragas.app.util.MunicipioQuery;
import com.ahorragas.app.util.RadiusUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel del mapa (MainActivity). Contiene la capa de datos: el repositorio,
 * la carga de gasolineras por radio, la búsqueda por municipio y el geocoding, y
 * el estado de datos asociado (todas las cargadas, las visibles para el
 * combustible actual, las de la búsqueda, el rango de precios y la última
 * consulta). Expone el resultado con {@link LiveData}; la Activity observa y se
 * encarga de pintar los markers y de la ubicación.
 */
public class MapViewModel extends AndroidViewModel {

    /** Resultado de recalcular las gasolineras visibles para un combustible. */
    public enum RadiusOutcome {
        /** Hay gasolineras que pintar. */
        SHOW,
        /** Hay datos cargados pero ninguno para el combustible seleccionado. */
        EMPTY_FUEL,
        /** No hay datos cargados en absoluto. */
        NO_DATA
    }

    /** Evento de un solo uso (evita re-entregar toasts/comandos al re-observar). */
    public static final class Event<T> {
        private final T content;
        private boolean handled;

        Event(T content) { this.content = content; }

        @Nullable
        public T getIfNotHandled() {
            if (handled) return null;
            handled = true;
            return content;
        }
    }

    private static final String PREF_SELECTED_FUEL = "pref_selected_fuel";

    private final EstacionRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    // ── Estado de datos (antes en MainActivity) ─────────────────────────────
    private final List<Gasolinera> allGasolineras = new ArrayList<>();
    private List<Gasolinera> visibleGasolineras = new ArrayList<>();
    private List<Gasolinera> searchGasolineras = null;
    private String lastSearchQuery = null;
    private Location searchLocation = null;
    private PriceRange currentPriceRange = new PriceRange(null, null, 0);
    private FuelType selectedFuel;

    // ── Salidas observables ─────────────────────────────────────────────────
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<RadiusOutcome>> radiusRender = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> searchRender = new MutableLiveData<>();
    private final MutableLiveData<Event<double[]>> centerOnSearch = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> toast = new MutableLiveData<>();

    public MapViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        GasolineraRepository gasolineraRepo =
                GasolineraRepository.getInstance(new RoomGasolineraDataSource(db));
        ElectrolineraRepository electrolineraRepo = ElectrolineraRepository.getInstance(
                new RemoteDgtDataSource(), new RoomElectrolineraDataSource(db));
        repository = EstacionRepository.getInstance(gasolineraRepo, electrolineraRepo);

        selectedFuel = FuelType.fromString(PreferenceManager.getDefaultSharedPreferences(app)
                .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name()));
    }

    // ── Observables ─────────────────────────────────────────────────────────
    public LiveData<Boolean> getLoading()                    { return loading; }
    public LiveData<Event<RadiusOutcome>> getRadiusRender()  { return radiusRender; }
    public LiveData<Event<Boolean>> getSearchRender()        { return searchRender; }
    public LiveData<Event<double[]>> getCenterOnSearch()     { return centerOnSearch; }
    public LiveData<Event<Integer>> getToast()               { return toast; }

    // ── Getters de estado (lectura desde la Activity) ───────────────────────
    public List<Gasolinera> getVisibleGasolineras() { return visibleGasolineras; }
    public PriceRange getCurrentPriceRange()         { return currentPriceRange; }
    public List<Gasolinera> getSearchGasolineras()   { return searchGasolineras; }
    public String getLastSearchQuery()               { return lastSearchQuery; }
    public boolean hasStations()                     { return !allGasolineras.isEmpty(); }
    public FuelType getSelectedFuel()                { return selectedFuel; }

    /** Fija el combustible sin recargar (el que llama decide qué hacer después). */
    public void setSelectedFuel(FuelType fuel) {
        selectedFuel = fuel != null ? fuel : FuelType.GASOLEO_A;
    }

    /**
     * Cambia el combustible y refresca la vista. Si el cambio cruza entre
     * eléctrico y combustión (son datasets distintos) y estamos en modo radio,
     * recarga desde el repositorio; en caso contrario recalcula las visibles.
     */
    public void changeFuel(@Nullable FuelType fuel, @Nullable Location userLocation) {
        FuelType previous = selectedFuel;
        selectedFuel = fuel != null ? fuel : FuelType.GASOLEO_A;

        boolean fuelTypeChanged = (previous == FuelType.ELECTRICO) != (selectedFuel == FuelType.ELECTRICO);
        if (fuelTypeChanged && lastSearchQuery == null && userLocation != null) {
            loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            return;
        }
        updateVisible();
    }

    /** Olvida la búsqueda activa (al pulsar "mi ubicación"). */
    public void clearSearch() {
        searchLocation = null;
        searchGasolineras = null;
        lastSearchQuery = null;
    }

    // ── Carga por radio ─────────────────────────────────────────────────────

    /**
     * Carga las gasolineras alrededor de las coordenadas dadas, calcula la
     * distancia, ordena por cercanía y recorta al máximo de markers. Al terminar
     * recalcula las visibles para el combustible y emite el resultado.
     */
    public void loadByRadius(double lat, double lon) {
        loading.setValue(true);
        final FuelType fuel = selectedFuel;
        Application app = getApplication();
        double radiusMeters = RadiusUtils.kmToMetersClamped(RadiusUtils.loadRadiusKm(app));

        executor.execute(() -> {
            try {
                List<Gasolinera> result = new ArrayList<>();
                if (fuel == FuelType.ELECTRICO) {
                    result.addAll(repository.getElectrolinerasByRadius(lat, lon, radiusMeters));
                } else {
                    result.addAll(repository.getGasolinerasByRadius(lat, lon, radiusMeters));
                }

                for (Gasolinera g : result) {
                    g.setDistanceMeters(GeoUtils.distanceMeters(lat, lon, g.getLat(), g.getLon()));
                }
                result.sort(Comparator.comparingDouble(g ->
                        g.getDistanceMeters() == null ? Double.MAX_VALUE : g.getDistanceMeters()));

                int maxMarkers = RadiusUtils.loadMarkersCount(app);
                if (result.size() > maxMarkers) {
                    result = result.subList(0, maxMarkers);
                }
                final List<Gasolinera> toShow = new ArrayList<>(result);

                main.post(() -> {
                    allGasolineras.clear();
                    allGasolineras.addAll(toShow);
                    loading.setValue(false);
                    emitVisible(fuel);
                });
            } catch (RepoError e) {
                main.post(() -> loading.setValue(false));
            }
        });
    }

    /**
     * Recalcula las gasolineras visibles para el combustible dado a partir de las
     * ya cargadas y emite el resultado (para cambios de combustible, descuentos o
     * favoritos que no requieren volver a consultar el repositorio).
     */
    public void updateVisible() {
        emitVisible(selectedFuel);
    }

    private void emitVisible(FuelType fuel) {
        visibleGasolineras = buildVisibleGasolineras(fuel);
        currentPriceRange = GasolineraSorter.calculatePriceRange(visibleGasolineras, fuel);
        for (Gasolinera g : visibleGasolineras) {
            g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(fuel), currentPriceRange));
        }

        RadiusOutcome outcome;
        if (allGasolineras.isEmpty()) {
            outcome = RadiusOutcome.NO_DATA;
        } else if (visibleGasolineras.isEmpty()) {
            outcome = RadiusOutcome.EMPTY_FUEL;
        } else {
            outcome = RadiusOutcome.SHOW;
        }
        radiusRender.setValue(new Event<>(outcome));
    }

    private List<Gasolinera> buildVisibleGasolineras(FuelType fuel) {
        List<Gasolinera> result = new ArrayList<>();
        for (Gasolinera g : allGasolineras) {
            if (fuel == FuelType.ELECTRICO && g.isElectric()) result.add(g);
            else if (fuel != FuelType.ELECTRICO && !g.isElectric() && g.hasPrice(fuel)) result.add(g);
        }
        return result;
    }

    // ── Búsqueda por municipio ──────────────────────────────────────────────

    /**
     * Geocodifica la localidad, centra el mapa (emitiendo su posición) y filtra
     * las gasolineras por municipio.
     */
    public void searchCity(String query, @Nullable Location userLocation) {
        loading.setValue(true);
        lastSearchQuery = query;
        final FuelType fuel = selectedFuel;
        executor.execute(() -> {
            try {
                double[] coords = GeocodingRepository.getInstance().geocodeCity(query);
                if (coords == null) {
                    main.post(() -> {
                        loading.setValue(false);
                        toast.setValue(new Event<>(R.string.msg_location_not_found));
                    });
                    return;
                }

                searchLocation = new Location("search");
                searchLocation.setLatitude(coords[0]);
                searchLocation.setLongitude(coords[1]);
                main.post(() -> centerOnSearch.setValue(new Event<>(new double[]{coords[0], coords[1]})));

                runMunicipioFilter(query, fuel, searchLocation);
            } catch (Exception e) {
                main.post(() -> {
                    loading.setValue(false);
                    toast.setValue(new Event<>(R.string.msg_error_searching_location));
                });
            }
        });
    }

    /**
     * Re-filtra por el municipio indicado sin geocodificar (para refrescar la
     * búsqueda activa cuando cambian radio, descuentos o favoritos).
     */
    public void filterByMunicipio(String query, @Nullable Location userLocation) {
        loading.setValue(true);
        lastSearchQuery = query;
        final FuelType fuel = selectedFuel;
        Location ref = searchLocation != null ? searchLocation : userLocation;
        executor.execute(() -> runMunicipioFilter(query, fuel, ref));
    }

    /** Cuerpo de la búsqueda por municipio; se ejecuta en el hilo del executor. */
    private void runMunicipioFilter(String query, FuelType fuel, @Nullable Location ref) {
        try {
            List<Gasolinera> result = new ArrayList<>();
            if (fuel == FuelType.ELECTRICO) {
                result.addAll(repository.getElectrolinerasByMunicipio(query));
                if (result.isEmpty()) {
                    result.addAll(repository.getElectrolinerasByMunicipio(
                            MunicipioQuery.stripLeadingArticle(query)));
                }
                if (result.isEmpty()) {
                    for (String variant : MunicipioQuery.invertedVariants(query)) {
                        result.addAll(repository.getElectrolinerasByMunicipio(variant));
                        if (!result.isEmpty()) break;
                    }
                }
            } else {
                result.addAll(repository.getGasolinerasByMunicipio(query));
                if (result.isEmpty()) {
                    result.addAll(repository.getGasolinerasByMunicipio(
                            MunicipioQuery.stripLeadingArticle(query)));
                }
                if (result.isEmpty()) {
                    for (String variant : MunicipioQuery.invertedVariants(query)) {
                        result.addAll(repository.getGasolinerasByMunicipio(variant));
                        if (!result.isEmpty()) break;
                    }
                }
            }

            if (ref != null) {
                for (Gasolinera g : result) {
                    g.setDistanceMeters(GeoUtils.distanceMeters(
                            ref.getLatitude(), ref.getLongitude(), g.getLat(), g.getLon()));
                }
                result.sort(Comparator.comparingDouble(g ->
                        g.getDistanceMeters() == null ? Double.MAX_VALUE : g.getDistanceMeters()));
            }

            final List<Gasolinera> filtered = GasolineraSorter.filterByFuel(result, fuel);
            currentPriceRange = GasolineraSorter.calculatePriceRange(filtered, fuel);
            for (Gasolinera g : filtered) {
                g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(fuel), currentPriceRange));
            }

            main.post(() -> {
                searchGasolineras = filtered;
                allGasolineras.clear();
                allGasolineras.addAll(filtered);
                loading.setValue(false);
                searchRender.setValue(new Event<>(filtered.isEmpty()));
            });
        } catch (RepoError e) {
            main.post(() -> {
                loading.setValue(false);
                toast.setValue(new Event<>(R.string.msg_error_searching_stations));
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
