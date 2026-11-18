package br.ucsal.medroutine.web.dto;

import br.ucsal.medroutine.domain.EstadoDose;

import java.time.Instant;
import java.util.List;

public record DoseDTO(Long id, Long idosoId, String idosoNome, Long medicamentoId, String medicamentoNome,
                      String dosagem, String orientacoes, Instant dataHoraPrevista, int toleranciaMinutos,
                      EstadoDose estado, ConfirmacaoDTO confirmacao, List<NotificacaoDTO> notificacoes) { }
