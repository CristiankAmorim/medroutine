package br.ucsal.medroutine.data.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.domain.EstadoDose;

public class Dose {
    public long id;
    public long idosoId;
    public String idosoNome;
    public long medicamentoId;
    public String medicamentoNome;
    public String dosagem;
    public String orientacoes;
    public Instant dataHoraPrevista;
    public int toleranciaMinutos;
    public String estado;
    public Confirmacao confirmacao;
    public List<Notificacao> notificacoes = new ArrayList<>();

    public EstadoDose estadoDose() {
        return EstadoDose.valueOf(estado);
    }
}
