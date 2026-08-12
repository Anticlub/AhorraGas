package com.ahorragas.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ahorragas.app.data.AppExecutors;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.util.DiscountPrefs;
import com.ahorragas.app.util.FavoritesPrefs;
import com.ahorragas.app.util.GasolineraSorter;
import com.ahorragas.app.util.GeoUtils;

import java.util.List;

/**
 * ViewModel de la pantalla "Favoritas". Carga las gasolineras favoritas desde
 * las preferencias, calcula la distancia (si hay ubicación), las ordena por
 * precio con descuento y asigna los niveles de precio. Expone el estado a la
 * Activity mediante un {@link LiveData}; la Activity solo observa, pinta y
 * gestiona las alertas/permisos (que son responsabilidad de la vista).
 */
public class FavoritesViewModel extends AndroidViewModel {

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

    private final MutableLiveData<UiState> state = new MutableLiveData<>();

    public FavoritesViewModel(@NonNull Application app) {
        super(app);
    }

    public LiveData<UiState> getState() {
        return state;
    }

    /**
     * Carga las gasolineras favoritas y las procesa para el combustible dado.
     * Si se pasan coordenadas, calcula la distancia a cada una; si son nulas
     * (sin ubicación) simplemente no muestra distancia.
     *
     * @param fuel combustible activo en el momento de la carga.
     * @param lat  latitud del usuario, o null si no hay ubicación.
     * @param lon  longitud del usuario, o null si no hay ubicación.
     */
    public void load(FuelType fuel, @Nullable Double lat, @Nullable Double lon) {
        state.setValue(UiState.loading());
        AppExecutors.io().execute(() -> {
            Application app = getApplication();
            List<Gasolinera> favorites = FavoritesPrefs.loadAll(app);
            if (favorites.isEmpty()) {
                state.postValue(UiState.empty());
                return;
            }

            if (lat != null && lon != null) {
                for (Gasolinera g : favorites) {
                    g.setDistanceMeters(GeoUtils.distanceMeters(lat, lon, g.getLat(), g.getLon()));
                }
            }

            PriceRange range = sortAndApplyLevels(favorites, fuel);
            state.postValue(UiState.data(favorites, range));
        });
    }

    /**
     * Ordena la lista in situ por precio con descuento (menor a mayor, las que no
     * tienen precio al final) y asigna a cada gasolinera su nivel de precio con
     * un rango calculado también sobre los precios ya descontados.
     */
    private PriceRange sortAndApplyLevels(List<Gasolinera> data, FuelType fuel) {
        Application app = getApplication();

        data.sort((a, b) -> {
            Double priceA = a.getPrecio(fuel);
            Double priceB = b.getPrecio(fuel);
            boolean aHasPrice = priceA != null && priceA > 0;
            boolean bHasPrice = priceB != null && priceB > 0;
            if (aHasPrice && bHasPrice) return Double.compare(
                    DiscountPrefs.applyAllDiscounts(app, a.getMarca(), priceA),
                    DiscountPrefs.applyAllDiscounts(app, b.getMarca(), priceB));
            if (aHasPrice) return -1;
            if (bHasPrice) return 1;
            return 0;
        });

        Double minPrice = null, maxPrice = null;
        for (Gasolinera g : data) {
            Double raw = g.getPrecio(fuel);
            if (raw == null || raw <= 0) continue;
            double discounted = DiscountPrefs.applyAllDiscounts(app, g.getMarca(), raw);
            if (minPrice == null || discounted < minPrice) minPrice = discounted;
            if (maxPrice == null || discounted > maxPrice) maxPrice = discounted;
        }
        PriceRange range = new PriceRange(minPrice, maxPrice, data.size());
        for (Gasolinera g : data) {
            Double raw = g.getPrecio(fuel);
            double discounted = raw != null ? DiscountPrefs.applyAllDiscounts(app, g.getMarca(), raw) : 0;
            g.setPriceLevel(GasolineraSorter.getPriceLevel(discounted, range));
        }
        return range;
    }
}
