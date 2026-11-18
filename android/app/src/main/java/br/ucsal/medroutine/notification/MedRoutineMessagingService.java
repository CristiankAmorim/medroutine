package br.ucsal.medroutine.notification;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.time.Instant;
import java.util.Map;

import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.data.sync.AtualizarAgendaWorker;
import br.ucsal.medroutine.session.SessionManager;

public class MedRoutineMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        SessionManager sessao = SessionManager.get(this);
        if (sessao.isLogado() && !sessao.isDemo()) {
            Repositorios.remoto(this).registrarTokenAparelho(token, new Callback<Void>() {
                @Override public void sucesso(Void r) { }
                @Override public void erro(ErroApi e) { }
            });
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensagem) {
        SessionManager sessao = SessionManager.get(this);
        if (!sessao.isLogado() || sessao.isDemo()) {
            return;
        }
        Map<String, String> d = mensagem.getData();
        String tipo = d.get("tipo");
        if ("AGENDA_ATUALIZADA".equals(tipo)) {
            if (sessao.isIdoso()) {
                AtualizarAgendaWorker.atualizarAgora(this);
            }
            return;
        }
        if (!sessao.isCuidador() || tipo == null || d.get("doseId") == null) {
            return;
        }
        String medicamento = d.get("medicamento") + " " + (d.get("dosagem") == null ? "" : d.get("dosagem"));
        Notificador.mostrarParaCuidador(this, tipo, Long.parseLong(d.get("doseId")), d.get("idosoNome"),
                medicamento.trim(), Instant.parse(d.get("previsto")), parseInt(d.get("toleranciaMinutos")),
                d.get("confirmadoEm") == null ? null : Instant.parse(d.get("confirmadoEm")),
                "true".equals(d.get("atraso")));
    }

    private static int parseInt(String s) {
        try {
            return s == null ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
