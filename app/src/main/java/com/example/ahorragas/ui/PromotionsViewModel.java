package com.example.ahorragas.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.ahorragas.data.repository.PromotionRepository;
import com.example.ahorragas.model.PromotionPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel para la pantalla de promociones de una gasolinera.
 * Expone el estado de carga y la lista de planes al Fragment.
 */
public class PromotionsViewModel extends ViewModel {

    public enum State { LOADING, SUCCESS, EMPTY, ERROR }

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>();
    private LiveData<List<PromotionPlan>> promotionsLiveData;

    private final PromotionRepository repository;

    public PromotionsViewModel() {
        repository = PromotionRepository.getInstance();
    }

    /**
     * Devuelve el LiveData con la lista de promociones.
     * La primera llamada lanza la descarga; las siguientes reutilizan el resultado.
     *
     * @return LiveData con la lista de PromotionPlan.
     */
    public LiveData<List<PromotionPlan>> getPromotions() {
        if (promotionsLiveData == null) {
            stateLiveData.setValue(State.LOADING);
            promotionsLiveData = repository.fetchPromotions();
        }
        return promotionsLiveData;
    }

    /**
     * Devuelve el LiveData con el estado actual de la pantalla.
     *
     * @return LiveData con el estado (LOADING, SUCCESS, EMPTY, ERROR).
     */
    public LiveData<State> getState() {
        return stateLiveData;
    }

    /**
     * Notifica al ViewModel el resultado de la descarga para que
     * actualice el estado de la pantalla.
     *
     * @param plans Lista recibida del repositorio (puede ser vacía).
     */
    public void onPromotionsLoaded(List<PromotionPlan> plans) {
        if (plans == null) {
            stateLiveData.setValue(State.ERROR);
        } else if (plans.isEmpty()) {
            stateLiveData.setValue(State.EMPTY);
        } else {
            stateLiveData.setValue(State.SUCCESS);
        }
    }

    // Mapa de equivalencias entre marca de la app → operador(es) del CSV
    private static final java.util.Map<String, List<String>> BRAND_ALIASES;
    static {
        BRAND_ALIASES = new java.util.HashMap<>();
        BRAND_ALIASES.put("bp",         java.util.Arrays.asList("bp oil"));
        BRAND_ALIASES.put("shell",      java.util.Arrays.asList("enilive"));
        BRAND_ALIASES.put("q8",         java.util.Arrays.asList("kuwait petroleum"));
        BRAND_ALIASES.put("cepsa",      java.util.Arrays.asList("moeve"));
        BRAND_ALIASES.put("moeve",      java.util.Arrays.asList("moeve"));
        BRAND_ALIASES.put("esso",       java.util.Arrays.asList("saras"));
        BRAND_ALIASES.put("disa",       java.util.Arrays.asList("disa"));
        BRAND_ALIASES.put("tamoil",     java.util.Arrays.asList("tamoil"));
        BRAND_ALIASES.put("repsol",     java.util.Arrays.asList("repsol"));
        BRAND_ALIASES.put("galp",       java.util.Arrays.asList("galp"));
        BRAND_ALIASES.put("carrefour",  java.util.Arrays.asList("carrefour"));
        BRAND_ALIASES.put("champion",   java.util.Arrays.asList("champion"));
    }

    /**
     * Filtra la lista de planes por la marca de la gasolinera.
     * Usa un mapa de equivalencias para cubrir casos donde el nombre
     * del operador en el CSV difiere del nombre de marca en la app.
     *
     * @param plans Lista completa de planes.
     * @param brand Marca de la gasolinera tal como viene de la API.
     * @return Lista filtrada, o lista vacía si no hay coincidencias.
     */
    public List<PromotionPlan> filterByBrand(List<PromotionPlan> plans, String brand) {
        if (plans == null) return null;
        if (brand == null || brand.trim().isEmpty()) return new ArrayList<>();

        String normalizedBrand = brand.trim().toLowerCase();

        // Buscar aliases para esta marca
        List<String> aliases = new ArrayList<>();
        for (java.util.Map.Entry<String, List<String>> entry : BRAND_ALIASES.entrySet()) {
            if (normalizedBrand.contains(entry.getKey())) {
                aliases.addAll(entry.getValue());
            }
        }
        // Si no hay alias definido, usar la propia marca como término de búsqueda
        if (aliases.isEmpty()) {
            aliases.add(normalizedBrand);
        }

        List<PromotionPlan> result = new ArrayList<>();
        for (PromotionPlan plan : plans) {
            String normalizedOperator = plan.getOperator().trim().toLowerCase();
            for (String alias : aliases) {
                if (normalizedOperator.contains(alias)) {
                    result.add(plan);
                    break;
                }
            }
        }
        return result;
    }
}