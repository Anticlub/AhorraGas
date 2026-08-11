package com.ahorragas.app.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executor compartido para trabajo en segundo plano (E/S, red, escrituras en
 * Room) que no está ligado al ciclo de vida de una pantalla concreta.
 *
 * Sustituye a los {@code new Thread(...)} sueltos, que se creaban sin control.
 * Es un pool compartido que vive durante todo el proceso.
 */
public final class AppExecutors {

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);

    private AppExecutors() {
    }

    /** Executor para tareas de fondo (E/S, red, Room). */
    public static ExecutorService io() {
        return IO;
    }
}
