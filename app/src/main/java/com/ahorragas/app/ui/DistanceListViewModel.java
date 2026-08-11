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
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.util.DiscountPrefs;
import com.ahorragas.app.util.GasolineraSorter;
import com.ahorragas.app.util.RadiusUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel de la pantalla "Por distancia". Contiene la carga y el procesado de
 * las gasolineras (repositorio, filtrado por combustible, orden por distancia,
 * cálculo de niveles de precio con descuento) y expone el estado a la Activity
 * mediante un {@link LiveData}. La Activity solo observa y pinta.
 */
public class DistanceListViewModel extends AndroidViewModel {

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

    public DistanceListViewModel(@NonNull Application app) {
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
            List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, fuel);
            PriceRange range = applyPriceLevels(filtered, fuel);
            state.postValue(filtered.isEmpty() ? UiState.empty() : UiState.data(filtered, range));
        });
    }

    /** Obtiene las gasolineras por radio desde el repositorio y las ordena por distancia. */
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

                List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, fuel);
                List<Gasolinera> sorted = GasolineraSorter.getWithinRadius(
                        filtered, lat, lon, radiusMeters, maxMarkers);

                PriceRange range = applyPriceLevels(sorted, fuel);
                state.postValue(sorted.isEmpty() ? UiState.empty() : UiState.data(sorted, range));
            } catch (Exception e) {
                state.postValue(UiState.error(
                        getApplication().getString(R.string.error_cargando_gasolineras)));
            }
        });
    }

    /**
     * Calcula el rango de precios y asigna a cada gasolinera su nivel de precio
     * teniendo en cuenta los descuentos configurados.
     */
    private PriceRange applyPriceLevels(List<Gasolinera> list, FuelType fuel) {
        PriceRange range = GasolineraSorter.calculatePriceRange(list, fuel);
        for (Gasolinera g : list) {
            double discounted = g.getPrecio(fuel) != null
                    ? DiscountPrefs.applyAllDiscounts(getApplication(), g.getMarca(), g.getPrecio(fuel))
                    : 0;
            g.setPriceLevel(GasolineraSorter.getPriceLevel(discounted, range));
        }
        return range;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
