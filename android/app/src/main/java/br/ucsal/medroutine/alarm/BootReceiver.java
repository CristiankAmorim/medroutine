package br.ucsal.medroutine.alarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.ucsal.medroutine.data.sync.AtualizarAgendaWorker;
import br.ucsal.medroutine.data.sync.FilaConfirmacoes;
import br.ucsal.medroutine.demo.DemoRepository;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.util.Executores;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String acao = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(acao)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(acao)
                && !AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(acao)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(acao)) {
            return;
        }
        Context app = context.getApplicationContext();
        SessionManager sessao = SessionManager.get(app);
        if (!sessao.isLogado()) {
            return;
        }
        PendingResult pendente = goAsync();
        Executores.disco(() -> {
            try {
                AgendadorLembretes.reagendarTodos(app);
                if (sessao.isDemo()) {
                    DemoRepository.get(app).agendarVerificacoes();
                } else if (sessao.isIdoso()) {
                    AtualizarAgendaWorker.atualizarAgora(app);
                    FilaConfirmacoes.agendarEnvio(app);
                }
            } finally {
                pendente.finish();
            }
        });
    }
}
