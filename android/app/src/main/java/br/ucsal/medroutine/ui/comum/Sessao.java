package br.ucsal.medroutine.ui.comum;

import android.app.Activity;
import android.content.Intent;

import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.sync.AtualizarAgendaWorker;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.inicio.BoasVindasActivity;
import br.ucsal.medroutine.util.Executores;

public final class Sessao {
    private Sessao() { }

    public static void encerrar(Activity activity) {
        android.content.Context app = activity.getApplicationContext();
        AtualizarAgendaWorker.cancelar(app);
        SessionManager.get(app).encerrar();
        Executores.disco(() -> {
            AppDatabase.get(app).limpar();
            AgendadorLembretes.cancelarTodos(app);
        });
        Intent i = new Intent(activity, BoasVindasActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
        activity.finish();
    }
}
