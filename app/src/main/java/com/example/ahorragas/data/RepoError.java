package com.example.ahorragas.data;


public class RepoError extends Exception {

    public enum Type {
        NETWORK,        // sin internet / corte
        TIMEOUT,        // tarda demasiado
        HTTP,           // 404, 500...
        EMPTY_RESPONSE, // respuesta OK pero lista vacía o null
        PARSE           // datos raros / inesperados
    }

    private final Type type;
    private final int httpCode; // solo si type=HTTP

    public RepoError(Type type, String message) {
        super(message);
        this.type = type;
        this.httpCode = -1;
    }

    public RepoError(Type type, int httpCode, String message) {
        super(message);
        this.type = type;
        this.httpCode = httpCode;
    }

    public Type getType() { return type; }
    public int getHttpCode() { return httpCode; }
}