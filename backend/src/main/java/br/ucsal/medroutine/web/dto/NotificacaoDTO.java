package br.ucsal.medroutine.web.dto;

import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.model.EstadoEnvio;
import br.ucsal.medroutine.model.TipoNotificacao;

import java.time.Instant;

public record NotificacaoDTO(Long id, Long doseId, TipoNotificacao tipo, EstadoEnvio estadoEnvio, int tentativas,
                             Instant criadoEm, Instant enviadoEm, String destinatario,
                             Long idosoId, String idosoNome, String medicamentoNome, String dosagem,
                             Instant dataHoraPrevista, int toleranciaMinutos, EstadoDose estadoDose) { }
