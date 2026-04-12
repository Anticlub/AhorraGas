package com.example.ahorragas.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Singleton que proporciona la instancia de OkHttpClient y
 * las interfaces Retrofit para cada API del proyecto.
 *
 * Centraliza timeouts, interceptores, logging y User-Agent
 * en un único punto de configuración.
 */
public final class ApiClient {

    private static final int CONNECT_TIMEOUT_S = 15;
    private static final int READ_TIMEOUT_S    = 60;
    private static final String USER_AGENT     = "AhorraGas/1.0 (Android)";

    private static ApiClient instance;

    private final OkHttpClient okHttpClient;

    private ApiClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()))
                .addInterceptor(logging)
                .build();
    }

    /**
     * Devuelve la instancia única de ApiClient.
     *
     * @return instancia singleton
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Crea una implementación de la interfaz Retrofit indicada
     * para la baseUrl dada. Usa ScalarsConverterFactory para
     * devolver respuestas como String.
     *
     * @param baseUrl  URL base del servicio (debe terminar en /)
     * @param service  clase de la interfaz Retrofit
     * @param <T>      tipo de la interfaz
     * @return implementación lista para usar
     */
    public <T> T createService(String baseUrl, Class<T> service) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        return retrofit.create(service);
    }
}