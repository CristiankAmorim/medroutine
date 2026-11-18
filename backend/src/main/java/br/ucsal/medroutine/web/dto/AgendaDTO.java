package br.ucsal.medroutine.web.dto;

import java.time.Instant;
import java.util.List;

public record AgendaDTO(UsuarioDTO idoso, Instant geradaEm, List<DoseDTO> doses) { }
