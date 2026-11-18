package br.ucsal.medroutine.data.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.local.ConfirmacaoPendente;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.demo.DemoRepository;
import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.notification.Notificador;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.util.Executores;

public final class FilaConfirmacoes {
    public static final String TRABALHO = "sincronizar_confirmacoes";

    public interface Resultado {
        void concluido(boolean ok);
    }

    private FilaConfirmacoes() { }

    public static void confirmar(Context context, long doseId, Resultado resultado) {
        Context app = context.getApplicationContext();
        Executores.disco(() -> {
            AppDatabase db = AppDatabase.get(app);
            DoseLocal dose = db.doses().porId(doseId);
            Instant agora = Instant.now();
            boolean ok = dose != null && !dose.isConfirmada() && RegrasDose.podeConfirmar(dose.previsto(), agora);
            if (dose != null && dose.isConfirmada()) {
                ok = true;
            } else if (ok) {
                EstadoDose estadoLocal = RegrasDose.estadoAposConfirmacao(dose.estadoDose(), dose.previsto(),
                        dose.toleranciaMinutos, agora);
                db.doses().atualizarEstado(doseId, estadoLocal.name(), agora.toEpochMilli(), true);
                AgendadorLembretes.cancelar(app, doseId);
                Notificador.cancelarLembrete(app, doseId);

                if (SessionManager.get(app).isDemo()) {
                    DemoRepository.get(app).confirmarComoIdoso(doseId, agora);
                    db.doses().atualizarEstado(doseId, estadoLocal.name(), agora.toEpochMilli(), false);
                } else {
                    db.confirmacoesPendentes().inserir(new ConfirmacaoPendente(doseId, agora.toEpochMilli()));
                    agendarEnvio(app);
                }
            }
            final boolean fim = ok;
            if (resultado != null) {
                Executores.principal(() -> resultado.concluido(fim));
            }
        });
    }

    public static void agendarEnvio(Context context) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SincronizarConfirmacoesWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(TRABALHO, ExistingWorkPolicy.REPLACE, req);
    }

    public static void agendarEnvioSeHouverPendentes(Context context) {
        if (!AppDatabase.get(context).confirmacoesPendentes().todas().isEmpty()) {
            agendarEnvio(context);
        }
    }
}
