package com.example.ahorragas.data.remote;

import com.example.ahorragas.data.GasolineraDataSource;
import com.example.ahorragas.data.GasolineraJsonParser;
import com.example.ahorragas.data.RepoError;
import com.example.ahorragas.data.RepoLogger;
import com.example.ahorragas.model.Gasolinera;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Fuente de datos remota que descarga gasolineras terrestres
 * del Ministerio usando Retrofit.
 */
public class RemoteApiDataSource implements GasolineraDataSource {

    private static final String BASE_URL =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/";

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1000L;

    private final GasolineraApiService apiService;

    public RemoteApiDataSource() {
        apiService = ApiClient.getInstance().createService(BASE_URL, GasolineraApiService.class);
    }

    /**
     * Descarga y parsea la lista de gasolineras, con hasta {@value MAX_RETRIES}
     * reintentos ante fallos de red o timeout.
     *
     * @return lista de gasolineras parseadas
     * @throws RepoError si todos los intentos fallan o el error no es reintentable
     */
    @Override
    public List<Gasolinera> loadGasolineras() throws RepoError {
        RepoError lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                String json = downloadJson();
                try {
                    return GasolineraJsonParser.parse(json);
                } catch (Exception e) {
                    throw new RepoError(
                            RepoError.Type.PARSE,
                            "Error parseando JSON: " + (e.getMessage() != null
                                    ? e.getMessage() : e.getClass().getSimpleName())
                    );
                }
            } catch (RepoError e) {
                boolean isRetryable = e.getType() == RepoError.Type.NETWORK
                        || e.getType() == RepoError.Type.TIMEOUT;

                if (!isRetryable || attempt > MAX_RETRIES) {
                    throw e;
                }

                lastError = e;
                RepoLogger.d("Intento " + attempt + " fallido (" + e.getType()
                        + "). Reintentando en " + RETRY_DELAY_MS + "ms…");

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw lastError;
    }

    /**
     * Ejecuta la llamada Retrofit y devuelve el JSON como String.
     *
     * @return cuerpo de la respuesta JSON
     * @throws RepoError si hay error de red, timeout, HTTP o respuesta vacía
     */
    private String downloadJson() throws RepoError {
        try {
            Response<String> response = apiService.getEstacionesTerrestres().execute();

            if (!response.isSuccessful()) {
                throw new RepoError(RepoError.Type.HTTP, response.code(),
                        "HTTP " + response.code());
            }

            String body = response.body();
            if (body == null || body.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE, "Respuesta remota vacía");
            }

            return body;

        } catch (RepoError e) {
            throw e;
        } catch (java.net.SocketTimeoutException e) {
            throw new RepoError(RepoError.Type.TIMEOUT, "Timeout de red");
        } catch (IOException e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Fallo de red: " + (e.getMessage() != null
                            ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}