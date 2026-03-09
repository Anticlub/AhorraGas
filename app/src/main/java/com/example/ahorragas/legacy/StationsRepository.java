package com.example.ahorragas.legacy;

import com.example.ahorragas.data.RepoError;
import com.example.ahorragas.data.RepoLogger;
import com.example.ahorragas.legacy.network.ApiClient;
import com.example.ahorragas.legacy.network.GasStation;
import com.example.ahorragas.legacy.network.GasStationsResponse;
import com.example.ahorragas.legacy.network.StationMapper;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StationsRepository {

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(RepoError error);
    }

    public void getStations(int idMunicipio, int idProducto, ResultCallback<List<Station>> callback) {

        RepoLogger.d("getStations municipio=" + idMunicipio + " producto=" + idProducto);

        ApiClient.getApi()
                .getStations(idMunicipio, idProducto)
                .enqueue(new Callback<GasStationsResponse>() {
                    @Override
                    public void onResponse(Call<GasStationsResponse> call, Response<GasStationsResponse> response) {

                        if (!response.isSuccessful()) {
                            RepoError err = new RepoError(
                                    RepoError.Type.HTTP,
                                    response.code(),
                                    "HTTP " + response.code() + " al pedir estaciones"
                            );
                            RepoLogger.d("ERROR " + err.getMessage());
                            callback.onError(err);
                            return;
                        }

                        GasStationsResponse body = response.body();
                        if (body == null || body.stations == null) {
                            RepoError err = new RepoError(
                                    RepoError.Type.EMPTY_RESPONSE,
                                    "Respuesta vacía (body o lista null)"
                            );
                            RepoLogger.d("ERROR " + err.getMessage());
                            callback.onError(err);
                            return;
                        }

                        if (body.stations.isEmpty()) {
                            RepoError err = new RepoError(
                                    RepoError.Type.EMPTY_RESPONSE,
                                    "Lista vacía (0 estaciones)"
                            );
                            RepoLogger.d("WARN " + err.getMessage());
                            callback.onError(err);
                            return;
                        }

                        List<Station> out = new ArrayList<>();
                        int mapped = 0;
                        int skipped = 0;

                        for (GasStation dto : body.stations) {
                            Station st = StationMapper.fromDto(dto);
                            if (st != null) {
                                out.add(st);
                                mapped++;
                            } else {
                                skipped++;
                            }
                        }

                        RepoLogger.d("OK estaciones API=" + body.stations.size()
                                + " mapeadas=" + mapped
                                + " descartadas=" + skipped);

                        callback.onSuccess(out);
                    }

                    @Override
                    public void onFailure(Call<GasStationsResponse> call, Throwable t) {

                        RepoLogger.e("Fallo de red", t);

                        // timeout
                        if (t instanceof SocketTimeoutException) {
                            callback.onError(new RepoError(RepoError.Type.TIMEOUT,
                                    "Timeout al conectar con la API"));
                            return;
                        }

                        // genérico red
                        callback.onError(new RepoError(RepoError.Type.NETWORK,
                                "Fallo de red: " + t.getClass().getSimpleName()));
                    }
                });
    }
}