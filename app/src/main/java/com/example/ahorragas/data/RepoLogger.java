package com.example.ahorragas.data;


import android.util.Log;

public class RepoLogger {

    private static final String TAG = "REPO";

    public static void d(String msg) {
        Log.d(TAG, msg);
    }

    public static void e(String msg, Throwable t) {
        Log.e(TAG, msg, t);
    }
}