package br.ucsal.medroutine.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Executores {
    private static final ExecutorService DISCO = Executors.newSingleThreadExecutor();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private Executores() { }

    public static void disco(Runnable r) {
        DISCO.execute(r);
    }

    public static void principal(Runnable r) {
        PRINCIPAL.post(r);
    }
}
