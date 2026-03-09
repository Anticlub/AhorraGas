package com.example.ahorragas.data;

import com.example.ahorragas.model.Gasolinera;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RemoteApiDataSource implements GasolineraDataSource {

    private static final String ENDPOINT =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/";

    @Override
    public List<Gasolinera> loadGasolineras() throws RepoError {
        String json = downloadJson();

        try {
            return GasolineraJsonParser.parse(json);
        } catch (Exception e) {
            throw new RepoError(
                    RepoError.Type.PARSE,
                    "Error parseando JSON: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
        }
    }

    protected String downloadJson() throws RepoError {
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Ahorragas/1.0 (Android)");

            int code = connection.getResponseCode();

            if (code >= 200 && code < 300) {
                is = connection.getInputStream();
            } else {
                is = connection.getErrorStream();
                String errorBody = (is != null) ? readStream(is) : "";
                throw new RepoError(RepoError.Type.HTTP, code,
                        "HTTP " + code + (errorBody.isEmpty() ? "" : " - " + safeShort(errorBody)));
            }

            String body = readStream(is);

            if (body == null || body.isEmpty()) {
                throw new RepoError(RepoError.Type.EMPTY_RESPONSE, "Respuesta remota vacía");
            }

            return body;

        } catch (SocketTimeoutException e) {
            throw new RepoError(RepoError.Type.TIMEOUT, "Timeout de red");

        } catch (RepoError e) {
            throw e;

        } catch (Exception e) {
            throw new RepoError(RepoError.Type.NETWORK,
                    "Fallo de red: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));

        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception ignored) {}

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
        );
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }

    private String safeShort(String s) {
        s = s.replace("\n", " ").replace("\r", " ").trim();
        if (s.length() > 200) return s.substring(0, 200) + "…";
        return s;
    }
}