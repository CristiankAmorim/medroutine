package br.ucsal.medroutine.data.repo;

import android.content.Context;

import br.ucsal.medroutine.demo.DemoRepository;
import br.ucsal.medroutine.session.SessionManager;

public final class Repositorios {
    private static RemoteRepository remoto;

    private Repositorios() { }

    public static MedRoutineRepository get(Context context) {
        if (SessionManager.get(context).isDemo()) {
            return DemoRepository.get(context);
        }
        return remoto(context);
    }

    public static synchronized RemoteRepository remoto(Context context) {
        if (remoto == null) {
            remoto = new RemoteRepository(context);
        }
        return remoto;
    }
}
