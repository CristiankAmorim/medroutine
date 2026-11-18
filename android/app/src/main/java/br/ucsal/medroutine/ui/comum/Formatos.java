package br.ucsal.medroutine.ui.comum;

import android.content.Context;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.domain.EstadoDose;

public final class Formatos {
    public static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", PT_BR);
    private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter DATA_LONGA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR);
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", PT_BR);

    private Formatos() { }

    public static ZoneId zona() {
        return ZoneId.systemDefault();
    }

    public static String hora(Instant instante) {
        return instante == null ? "--:--" : HORA.format(instante.atZone(zona()));
    }

    public static String dataHora(Instant instante) {
        return instante == null ? "" : DATA_HORA.format(instante.atZone(zona()));
    }

    public static String data(LocalDate data) {
        return data == null ? "" : DATA_CURTA.format(data);
    }

    public static String data(Instant instante) {
        return instante == null ? "" : DATA_CURTA.format(instante.atZone(zona()));
    }

    public static String dataLonga(LocalDate data) {
        String s = DATA_LONGA.format(data);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String diaMes(LocalDate data) {
        return DIA_MES.format(data);
    }

    public static LocalDate hoje() {
        return LocalDate.now(zona());
    }

    public static String saudacao(Context context) {
        int h = LocalTime.now(zona()).getHour();
        if (h < 12) return context.getString(R.string.bom_dia);
        if (h < 18) return context.getString(R.string.boa_tarde);
        return context.getString(R.string.boa_noite);
    }

    public static String nomeEstado(Context context, String estado) {
        if (estado == null) return "";
        switch (EstadoDose.valueOf(estado)) {
            case CONFIRMADA: return context.getString(R.string.estado_confirmada);
            case NAO_CONFIRMADA: return context.getString(R.string.estado_nao_confirmada);
            case CONFIRMADA_COM_ATRASO: return context.getString(R.string.estado_atraso);
            default: return context.getString(R.string.estado_pendente);
        }
    }

    public static int corEstado(String estado) {
        if (estado == null) return R.color.texto_secundario;
        switch (EstadoDose.valueOf(estado)) {
            case CONFIRMADA: return R.color.confirmacao;
            case NAO_CONFIRMADA: return R.color.alerta;
            case CONFIRMADA_COM_ATRASO: return R.color.alerta;
            default: return R.color.texto_secundario;
        }
    }

    public static int iconeEstado(String estado) {
        if (estado == null) return R.drawable.ic_relogio;
        switch (EstadoDose.valueOf(estado)) {
            case CONFIRMADA: return R.drawable.ic_check;
            case NAO_CONFIRMADA: return R.drawable.ic_alerta;
            case CONFIRMADA_COM_ATRASO: return R.drawable.ic_relogio;
            default: return R.drawable.ic_relogio;
        }
    }

    public static String frequencia(Context context, String tipo, Integer intervalo) {
        if ("A_CADA_N_HORAS".equals(tipo)) return context.getString(R.string.freq_a_cada_n_resumo, intervalo == null ? 0 : intervalo);
        if ("DIAS_SEMANA".equals(tipo)) return context.getString(R.string.freq_dias_semana);
        return context.getString(R.string.freq_diaria);
    }
}
