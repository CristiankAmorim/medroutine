package br.ucsal.medroutine.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.notification.Notificador;
import br.ucsal.medroutine.util.Executores;

public class LembreteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long doseId = intent.getLongExtra(AgendadorLembretes.EXTRA_DOSE_ID, -1);
        if (doseId < 0) {
            return;
        }
        PendingResult pendente = goAsync();
        Context app = context.getApplicationContext();
        Executores.disco(() -> {
            try {
                DoseLocal dose = AppDatabase.get(app).doses().porId(doseId);
                if (dose != null && !dose.isConfirmada()) {
                    Notificador.mostrarLembrete(app, dose);
                }
            } finally {
                pendente.finish();
            }
        });
    }
}
