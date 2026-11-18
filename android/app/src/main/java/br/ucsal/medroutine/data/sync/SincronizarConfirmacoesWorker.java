package br.ucsal.medroutine.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.local.ConfirmacaoPendente;
import br.ucsal.medroutine.data.model.ConfirmacaoResponse;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.session.SessionManager;
import retrofit2.Response;

public class SincronizarConfirmacoesWorker extends Worker {
    public SincronizarConfirmacoesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (!SessionManager.get(ctx).isLogado() || SessionManager.get(ctx).isDemo()) {
            return Result.success();
        }
        AppDatabase db = AppDatabase.get(ctx);
        List<ConfirmacaoPendente> fila = db.confirmacoesPendentes().todas();
        boolean repetir = false;
        for (ConfirmacaoPendente c : fila) {
            try {
                Response<ConfirmacaoResponse> resp = Repositorios.remoto(ctx)
                        .confirmarSincrono(c.doseId, Instant.ofEpochMilli(c.horarioAparelho));
                if (resp.isSuccessful() && resp.body() != null) {
                    ConfirmacaoResponse r = resp.body();
                    Long confirmadaEm = c.horarioAparelho;
                    db.doses().atualizarEstado(c.doseId, r.dose.estado, confirmadaEm, false);
                    db.confirmacoesPendentes().remover(c.doseId);
                } else if (resp.code() >= 500 || resp.code() == 401 || resp.code() == 429) {
                    db.confirmacoesPendentes().incrementarTentativas(c.doseId);
                    repetir = true;
                } else {
                    db.confirmacoesPendentes().remover(c.doseId);
                    db.doses().atualizarEstado(c.doseId, "PENDENTE", null, false);
                }
            } catch (IOException e) {
                repetir = true;
            }
        }
        return repetir ? Result.retry() : Result.success();
    }
}
