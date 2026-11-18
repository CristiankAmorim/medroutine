package br.ucsal.medroutine.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.sync.FilaConfirmacoes;
import br.ucsal.medroutine.notification.Notificador;

public class AcaoLembreteReceiver extends BroadcastReceiver {
    public static final String ACAO_CONFIRMAR = "br.ucsal.medroutine.CONFIRMAR";
    public static final String ACAO_ADIAR = "br.ucsal.medroutine.ADIAR";

    @Override
    public void onReceive(Context context, Intent intent) {
        long doseId = intent.getLongExtra(AgendadorLembretes.EXTRA_DOSE_ID, -1);
        if (doseId < 0) {
            return;
        }
        Context app = context.getApplicationContext();
        Notificador.cancelarLembrete(app, doseId);
        if (ACAO_CONFIRMAR.equals(intent.getAction())) {
            PendingResult pendente = goAsync();
            FilaConfirmacoes.confirmar(app, doseId, ok -> {
                Toast.makeText(app, ok ? R.string.lembrete_confirmado : R.string.lembrete_cedo, Toast.LENGTH_LONG).show();
                pendente.finish();
            });
        } else if (ACAO_ADIAR.equals(intent.getAction())) {
            AgendadorLembretes.adiar(app, doseId);
            Toast.makeText(app, R.string.lembrete_adiado, Toast.LENGTH_SHORT).show();
        }
    }
}
