package com.ahorragas.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ahorragas.app.R;
import com.ahorragas.app.data.ElectrolineraRepository;
import com.ahorragas.app.data.EstacionRepository;
import com.ahorragas.app.data.GasolineraRepository;
import com.ahorragas.app.data.RoomElectrolineraDataSource;
import com.ahorragas.app.data.RoomGasolineraDataSource;
import com.ahorragas.app.data.local.AppDatabase;
import com.ahorragas.app.data.remote.RemoteDgtDataSource;
import com.ahorragas.app.model.Electrolinera;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.util.BrandPrefs;
import com.ahorragas.app.util.DiscountPrefs;
import com.ahorragas.app.util.GasolineraSorter;
import com.ahorragas.app.util.RadiusUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel de la pantalla "Por precio". Contiene la carga y el procesado de las
 * gasolineras (repositorio, filtrado por combustible, orden por precio con
 * descuento —o por potencia si es eléctrico— y cálculo de niveles de precio) y
 * expone el estado a la Activity mediante un {@link LiveData}. La Activity solo
 * observa y pinta.
 */
public class PriceListViewModel extends AndroidViewModel {

    /** Estado de la pantalla. */
    public enum Status { LOADING, DATA, EMPTY, ERROR }

    /** Instantánea inmutable del estado de la UI. */
    public static final class UiState {
        public final Status status;
        public final List<Gasolinera> data;   // solo en DATA
        public final PriceRange priceRange;    // solo en DATA
        public final String errorMessage;      // solo en ERROR

        private UiState(Status status, List<Gasolinera> data,
                        PriceRange priceRange, String errorMessage) {
            this.status = status;
            this.data = data;
            this.priceRange = priceRange;
            this.errorMessage = errorMessage;
        }

        static UiState loading()                              { return new UiState(Status.LOADING, null, null, null); }
        static UiState data(List<Gasolinera> d, PriceRange r) { return new UiState(Status.DATA, d, r, null); }
        static UiState empty()                                { return new UiState(Status.EMPTY, null, null, null); }
        static UiState error(String msg)                      { return new UiState(Status.ERROR, null, null, msg); }
    }

    private final EstacionRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();

    public PriceListViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        GasolineraRepository gasolineraRepo =
                GasolineraRepository.getInstance(new RoomGasolineraDataSource(db));
        ElectrolineraRepository electrolineraRepo = ElectrolineraRepository.getInstance(
                new RemoteDgtDataSource(), new RoomElectrolineraDataSource(db));
        repository = EstacionRepository.getInstance(gasolineraRepo, electrolineraRepo);
    }

    public LiveData<UiState> getState() {
        return state;
    }

    /** Procesa una lista ya obtenida (p. ej. desde el Intent de una búsqueda). */
    public void loadFromIntent(List<Gasolinera> gasolineras, FuelType fuel) {
        state.setValue(UiState.loading());
        executor.execute(() -> {
            List<Gasolinera> filtered = GasolineraSorter.filterByBrand(
                    GasolineraSorter.filterByFuel(gasolineras, fuel),
                    BrandPrefs.get(getApplication()));
            PriceRange range = sortAndApplyLevels(filtered, fuel);
            state.postValue(filtered.isEmpty() ? UiState.empty() : UiState.data(filtered, range));
        });
    }

    /** Obtiene las gasolineras por radio desde el repositorio y las ordena por precio. */
    public void loadByRadius(double lat, double lon, FuelType fuel) {
        state.setValue(UiState.loading());
        executor.execute(() -> {
            try {
                Application app = getApplication();
                double radiusMeters = RadiusUtils.kmToMetersClamped(RadiusUtils.loadRadiusKm(app));
                int maxMarkers = RadiusUtils.loadMarkersCount(app);

                List<Gasolinera> gasolineras = fuel == FuelType.ELECTRICO
                        ? new ArrayList<>(repository.getElectrolinerasByRadius(lat, lon, radiusMeters))
                        : new ArrayList<>(repository.getGasolinerasByRadius(lat, lon, radiusMeters));

                List<Gasolinera> filtered = GasolineraSorter.filterByBrand(
                        GasolineraSorter.filterByFuel(gasolineras, fuel), BrandPrefs.get(app));
                List<Gasolinera> inRadius = GasolineraSorter.getWithinRadius(
                        filtered, lat, lon, radiusMeters, maxMarkers);

                PriceRange range = sortAndApplyLevels(inRadius, fuel);
                state.postValue(inRadius.isEmpty() ? UiState.empty() : UiState.data(inRadius, range));
            } catch (Exception e) {
                state.postValue(UiState.error(
                        getApplication().getString(R.string.error_cargando_gasolineras)));
            }
        });
    }

    /**
     * Ordena la lista in situ y asigna a cada gasolinera su nivel de precio.
     * Para combustible eléctrico ordena por potencia máxima descendente y no hay
     * niveles de precio (devuelve un rango vacío); para el resto ordena por precio
     * con descuento ascendente y calcula min/max y niveles.
     */
    private PriceRange sortAndApplyLevels(List<Gasolinera> list, FuelType fuel) {
        if (fuel == FuelType.ELECTRICO) {
            list.sort((a, b) -> Double.compare(getMaxPotencia(b), getMaxPotencia(a)));
            return new PriceRange(null, null, 0);
        }

        Application app = getApplication();
        list.sort(Comparator.comparingDouble(g ->
                g.getPrecio(fuel) != null
                        ? DiscountPrefs.applyAllDiscounts(app, g.getMarca(), g.getPrecio(fuel))
                        : Double.MAX_VALUE));

        PriceRange range = GasolineraSorter.calculatePriceRange(list, fuel);
        for (Gasolinera g : list) {
            double discounted = g.getPrecio(fuel) != null
                    ? DiscountPrefs.applyAllDiscounts(app, g.getMarca(), g.getPrecio(fuel))
                    : 0;
            g.setPriceLevel(GasolineraSorter.getPriceLevel(discounted, range));
        }
        return range;
    }

    /**
     * Devuelve la potencia máxima en vatios de una electrolinera.
     * Devuelve 0 si no tiene conectores o no es eléctrica.
     */
    private double getMaxPotencia(Gasolinera g) {
        if (g.getConectores() == null || g.getConectores().isEmpty()) return 0;
        double max = 0;
        for (Electrolinera.Conector c : g.getConectores()) {
            if (c.getPotenciaW() != null && c.getPotenciaW() > max) {
                max = c.getPotenciaW();
            }
        }
        return max;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
