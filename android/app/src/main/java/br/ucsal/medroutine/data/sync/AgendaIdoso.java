package br.ucsal.medroutine.data.sync;

import android.content.Context;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.data.model.Agenda;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.util.Executores;

public final class AgendaIdoso {
    private AgendaIdoso() { }

    public static void atualizar(Context context, Callback<List<DoseLocal>> cb) {
        Context app = context.getApplicationContext();
        Repositorios.get(app).agenda(new Callback<Agenda>() {
            @Override
            public void sucesso(Agenda agenda) {
                Executores.disco(() -> {
                    salvar(app, agenda);
                    List<DoseLocal> hoje = dosesDeHojeSincrono(app);
                    Executores.principal(() -> cb.sucesso(hoje));
                });
            }

            @Override
            public void erro(ErroApi erro) {
                Executores.disco(() -> {
                    List<DoseLocal> hoje = dosesDeHojeSincrono(app);
                    Executores.principal(() -> {
                        if (hoje.isEmpty()) {
                            cb.erro(erro);
                        } else {
                            cb.sucesso(hoje);
                        }
                    });
                });
            }
        });
    }

    public static void salvar(Context context, Agenda agenda) {
        SessionManager sessao = SessionManager.get(context);
        if (agenda.idoso != null) {
            sessao.setAnuncioVoz(agenda.idoso.anuncioVoz);
            sessao.setNome(agenda.idoso.nome);
        }
        List<DoseLocal> doses = new ArrayList<>();
        for (Dose d : agenda.doses) {
            doses.add(DoseLocal.de(d));
        }
        AppDatabase.get(context).doses().substituirAgenda(doses);
        AgendadorLembretes.reagendarTodos(context);
    }

    public static List<DoseLocal> dosesDeHojeSincrono(Context context) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate hoje = LocalDate.now(zona);
        long inicio = hoje.atStartOfDay(zona).toInstant().toEpochMilli();
        long fim = hoje.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli();
        return AppDatabase.get(context).doses().doPeriodo(inicio, fim);
    }
}
