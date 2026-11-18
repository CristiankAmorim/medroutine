package br.ucsal.medroutine.service;

import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.model.ConfirmacaoDose;
import br.ucsal.medroutine.model.DoseProgramada;
import br.ucsal.medroutine.model.Notificacao;
import br.ucsal.medroutine.model.TipoNotificacao;
import br.ucsal.medroutine.model.Usuario;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

final class MensagensNotificacao {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private MensagensNotificacao() { }

    static Map<String, String> montar(Notificacao n, ConfirmacaoDose confirmacao) {
        DoseProgramada dose = n.getDose();
        Usuario idoso = dose.getIdoso();
        ZoneId zona = idoso.zona();
        String medicamento = dose.getMedicamento().getNome() + " " + dose.getMedicamento().getDosagem();
        String previsto = HH_MM.format(dose.getDataHoraPrevista().atZone(zona));
        int tolerancia = dose.getHorario().getToleranciaMinutos();

        Map<String, String> dados = new HashMap<>();
        dados.put("tipo", n.getTipo().name());
        dados.put("notificacaoId", String.valueOf(n.getId()));
        dados.put("doseId", String.valueOf(dose.getId()));
        dados.put("idosoId", String.valueOf(idoso.getId()));
        dados.put("idosoNome", idoso.getNome());
        dados.put("medicamento", dose.getMedicamento().getNome());
        dados.put("dosagem", dose.getMedicamento().getDosagem());
        dados.put("previsto", dose.getDataHoraPrevista().toString());
        dados.put("toleranciaMinutos", String.valueOf(tolerancia));

        if (n.getTipo() == TipoNotificacao.CONFIRMACAO && confirmacao != null) {
            boolean atraso = dose.getEstado() == EstadoDose.CONFIRMADA_COM_ATRASO;
            Instant momento = RegrasDose.momentoDaConfirmacao(dose.getDataHoraPrevista(),
                    confirmacao.getConfirmadoEm(), confirmacao.getHorarioAparelho());
            String hora = HH_MM.format(momento.atZone(zona));
            dados.put("confirmadoEm", momento.toString());
            dados.put("atraso", String.valueOf(atraso));
            dados.put("peloCuidador", String.valueOf(confirmacao.isPeloCuidador()));
            dados.put("titulo", atraso ? "Dose confirmada com atraso" : "Dose confirmada");
            dados.put("corpo", atraso
                    ? idoso.getNome() + " confirmou com atraso " + medicamento + " das " + previsto + ", às " + hora + "."
                    : idoso.getNome() + " confirmou " + medicamento + " às " + hora + "."
                    + (momento.isBefore(confirmacao.getConfirmadoEm().minusSeconds(60))
                        ? " A confirmação foi feita sem conexão e recebida às "
                            + HH_MM.format(confirmacao.getConfirmadoEm().atZone(zona)) + "."
                        : ""));
        } else {
            String limite = HH_MM.format(dose.getDataHoraPrevista().plusSeconds(tolerancia * 60L).atZone(zona));
            dados.put("titulo", "Dose não confirmada");
            dados.put("corpo", idoso.getNome() + " não confirmou " + medicamento + " das " + previsto
                    + ". A tolerância de " + tolerancia + " minutos terminou às " + limite + ".");
        }
        return dados;
    }
}
