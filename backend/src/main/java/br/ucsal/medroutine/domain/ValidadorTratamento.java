package br.ucsal.medroutine.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class ValidadorTratamento {
    public static final int TOLERANCIA_MINIMA = 5;
    public static final int TOLERANCIA_MAXIMA = 240;
    public static final int TOLERANCIA_PADRAO = 30;
    public static final int INTERVALO_MINIMO_HORAS = 1;
    public static final int INTERVALO_MAXIMO_HORAS = 24;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private ValidadorTratamento() { }

    public static int toleranciaOuPadrao(Integer toleranciaMinutos) {
        if (toleranciaMinutos == null) {
            return TOLERANCIA_PADRAO;
        }
        if (toleranciaMinutos < TOLERANCIA_MINIMA || toleranciaMinutos > TOLERANCIA_MAXIMA) {
            throw new ValidacaoException("toleranciaMinutos",
                    "A tolerância deve ficar entre " + TOLERANCIA_MINIMA + " e " + TOLERANCIA_MAXIMA + " minutos.");
        }
        return toleranciaMinutos;
    }

    public static LocalTime parseHora(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new ValidacaoException("hora", "Informe o horário.");
        }
        try {
            return LocalTime.parse(texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new ValidacaoException("hora", "Horário inválido. Use o formato HH:mm, por exemplo 08:00.");
        }
    }

    public static void validarMedicamento(String nome, String dosagem, LocalDate inicio, LocalDate fim) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new ValidacaoException("nome", "Informe o nome do medicamento.");
        }
        if (nome.trim().length() > 120) {
            throw new ValidacaoException("nome", "O nome deve ter no máximo 120 caracteres.");
        }
        if (dosagem == null || dosagem.trim().isEmpty()) {
            throw new ValidacaoException("dosagem", "Informe a dosagem.");
        }
        if (inicio == null) {
            throw new ValidacaoException("dataInicio", "Informe a data de início.");
        }
        if (fim != null && fim.isBefore(inicio)) {
            throw new ValidacaoException("dataFim", "A data de término não pode ser anterior à data de início.");
        }
    }

    public static void validarHorario(RegraHorario regra) {
        if (regra.getHora() == null) {
            throw new ValidacaoException("hora", "Informe o horário.");
        }
        if (regra.getTipoFrequencia() == null) {
            throw new ValidacaoException("tipoFrequencia", "Escolha a frequência.");
        }
        toleranciaOuPadrao(regra.getToleranciaMinutos());
        switch (regra.getTipoFrequencia()) {
            case A_CADA_N_HORAS:
                Integer n = regra.getIntervaloHoras();
                if (n == null || n < INTERVALO_MINIMO_HORAS || n > INTERVALO_MAXIMO_HORAS) {
                    throw new ValidacaoException("intervaloHoras",
                            "O intervalo deve ficar entre " + INTERVALO_MINIMO_HORAS + " e "
                                    + INTERVALO_MAXIMO_HORAS + " horas.");
                }
                break;
            case DIAS_SEMANA:
                if (regra.getDiasSemana().isEmpty()) {
                    throw new ValidacaoException("diasSemana", "Selecione ao menos um dia da semana.");
                }
                break;
            default:
                break;
        }
    }
}
