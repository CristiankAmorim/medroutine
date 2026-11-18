package br.ucsal.medroutine.web.dto;

import java.time.Instant;

public record ConfirmacaoDTO(Instant confirmadoEm, Instant horarioAparelho, String confirmadoPor,
                             boolean peloCuidador) { }
