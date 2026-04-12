package com.example.ahorragas.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ahorragas.data.remote.ApiClient;
import com.example.ahorragas.data.remote.PrecioilApiService;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.PriceHistoryEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Repositorio que obtiene el histórico de precios de una estación
 * desde la API de Precioil y lo expone como LiveData.
 */
public class PriceHistoryRepository {

    private static final String BASE_URL = "https://api.precioil.es/";

    private final PrecioilApiService apiService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PriceHistoryRepository() {
        apiService = ApiClient.getInstance().createService(BASE_URL, PrecioilApiService.class);
    }

    /**
     * Obtiene el histórico de precios de una estación, filtrado por
     * un tipo de combustible concreto.
     *
     * @param stationId   ID de la estación (IDEESS)
     * @param fuelType    tipo de combustible a filtrar
     * @param fechaInicio fecha de inicio (yyyy-MM-dd)
     * @param fechaFin    fecha de fin (yyyy-MM-dd)
     * @return LiveData con la lista de entradas del histórico, o lista vacía si hay error
     */
    public LiveData<List<PriceHistoryEntry>> getHistory(int stationId, FuelType fuelType,
                                                        String fechaInicio, String fechaFin) {
        MutableLiveData<List<PriceHistoryEntry>> liveData = new MutableLiveData<>();

        executor.execute(() -> {
            List<PriceHistoryEntry> entries = new ArrayList<>();

            try {
                Response<String> response = apiService
                        .getHistorico(stationId, fechaInicio, fechaFin).execute();

                if (response.isSuccessful() && response.body() != null) {
                    entries = parseResponse(response.body(), fuelType);
                }
            } catch (Exception e) {
                android.util.Log.e("PriceHistoryRepo",
                        "Error obteniendo histórico: " + e.getMessage(), e);
            }

            liveData.postValue(entries);
        });

        return liveData;
    }

    /**
     * Parsea la respuesta JSON y filtra solo las entradas del combustible indicado.
     *
     * @param json     respuesta JSON completa de la API
     * @param fuelType combustible a filtrar
     * @return lista de entradas filtradas
     */
    private List<PriceHistoryEntry> parseResponse(String json, FuelType fuelType) {
        List<PriceHistoryEntry> result = new ArrayList<>();

        try {
            JSONObject root = new JSONObject(json);
            JSONArray data = root.optJSONArray("data");
            if (data == null) return result;

            int targetId = fuelType.precioilId();

            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                int idFuel = item.optInt("idFuelType", -1);

                if (idFuel != targetId) continue;

                String priceStr = item.optString("precio", "0");
                double price = Double.parseDouble(priceStr);
                String timestamp = item.optString("timestamp", "");

                result.add(new PriceHistoryEntry(fuelType, price, timestamp));
            }
        } catch (Exception e) {
            android.util.Log.e("PriceHistoryRepo",
                    "Error parseando histórico: " + e.getMessage(), e);
        }

        return result;
    }
}