package br.ucsal.medroutine;

import android.app.Application;

import br.ucsal.medroutine.notification.Notificador;

public class MedRoutineApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Notificador.criarCanais(this);
    }
}
