package br.ucsal.medroutine.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.ucsal.medroutine.util.Executores;

public class DemoVerificacaoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendente = goAsync();
        Context app = context.getApplicationContext();
        Executores.disco(() -> {
            try {
                DemoRepository.get(app).verificarAgora();
            } finally {
                pendente.finish();
            }
        });
    }
}
