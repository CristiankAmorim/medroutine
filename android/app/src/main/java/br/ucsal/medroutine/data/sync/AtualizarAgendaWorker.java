package br.ucsal.medroutine.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import br.ucsal.medroutine.data.model.Agenda;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.session.SessionManager;
import retrofit2.Response;

public class AtualizarAgendaWorker extends Worker {
    private static final String PERIODICO = "agenda_periodica";
    private static final String IMEDIATO = "agenda_imediata";

    public AtualizarAgendaWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void agendarPeriodico(Context context) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(AtualizarAgendaWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODICO, ExistingPeriodicWorkPolicy.KEEP, req);
    }

    public static void atualizarAgora(Context context) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(AtualizarAgendaWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(IMEDIATO, ExistingWorkPolicy.REPLACE, req);
    }

    public static void cancelar(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODICO);
        WorkManager.getInstance(context).cancelUniqueWork(IMEDIATO);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SessionManager sessao = SessionManager.get(ctx);
        if (!sessao.isLogado() || !sessao.isIdoso() || sessao.isDemo()) {
            return Result.success();
        }
        FilaConfirmacoes.agendarEnvioSeHouverPendentes(ctx);
        try {
            Response<Agenda> resp = Repositorios.remoto(ctx).agendaSincrona();
            if (resp.isSuccessful() && resp.body() != null) {
                AgendaIdoso.salvar(ctx, resp.body());
                return Result.success();
            }
            return resp.code() >= 500 ? Result.retry() : Result.failure();
        } catch (IOException e) {
            return Result.retry();
        }
    }
}
