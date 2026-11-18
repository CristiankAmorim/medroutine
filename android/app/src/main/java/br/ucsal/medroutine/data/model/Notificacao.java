package br.ucsal.medroutine.data.model;

import java.time.Instant;

public class Notificacao {
    public static final String CONFIRMACAO = "CONFIRMACAO";
    public static final String ALERTA = "ALERTA";

    public long id;
    public long doseId;
    public String tipo;
    public String estadoEnvio;
    public int tentativas;
    public Instant criadoEm;
    public Instant enviadoEm;
    public String destinatario;
    public long idosoId;
    public String idosoNome;
    public String medicamentoNome;
    public String dosagem;
    public Instant dataHoraPrevista;
    public int toleranciaMinutos;
    public String estadoDose;
}
