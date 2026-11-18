package br.ucsal.medroutine.ui.cuidador;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Horario;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.data.model.MedicamentoRequest;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.domain.RegraHorario;
import br.ucsal.medroutine.domain.TipoFrequencia;
import br.ucsal.medroutine.domain.ValidacaoException;
import br.ucsal.medroutine.domain.ValidadorTratamento;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Ui;

public class MedicamentoFormActivity extends AppCompatActivity {
    private static final String EXTRA_IDOSO = "idosoId";
    private static final String EXTRA_NOME_IDOSO = "nomeIdoso";
    private static final String EXTRA_MEDICAMENTO = "medicamentoId";

    private long idosoId;
    private Long medicamentoId;
    private final List<Horario> horarios = new ArrayList<>();
    private TipoFrequencia frequencia = TipoFrequencia.DIARIA;
    private int toleranciaPadrao = ValidadorTratamento.TOLERANCIA_PADRAO;
    private LocalDate dataInicio = Formatos.hoje();
    private LocalDate dataFim;

    private TextInputLayout campoNome, campoDosagem, campoOrientacoes, campoIntervalo, campoInicio, campoFim;
    private EditText nome, dosagem, orientacoes, intervalo, entradaInicio, entradaFim;
    private ChipGroup chipsHorarios, chipsDias, chipsTolerancia;
    private MaterialButton botaoAddHorario;
    private final Map<Integer, DayOfWeek> chipDia = new LinkedHashMap<>();

    public static Intent novo(Context c, long idosoId, String nomeIdoso) {
        return new Intent(c, MedicamentoFormActivity.class)
                .putExtra(EXTRA_IDOSO, idosoId)
                .putExtra(EXTRA_NOME_IDOSO, nomeIdoso);
    }

    public static Intent editar(Context c, long idosoId, String nomeIdoso, long medicamentoId) {
        return novo(c, idosoId, nomeIdoso).putExtra(EXTRA_MEDICAMENTO, medicamentoId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicamento_form);
        idosoId = getIntent().getLongExtra(EXTRA_IDOSO, 0);
        long med = getIntent().getLongExtra(EXTRA_MEDICAMENTO, -1);
        medicamentoId = med > 0 ? med : null;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(medicamentoId == null ? R.string.novo_medicamento : R.string.editar_medicamento);
        toolbar.setSubtitle(getString(R.string.para_idoso, getIntent().getStringExtra(EXTRA_NOME_IDOSO)));
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));

        campoNome = findViewById(R.id.campo_nome);
        campoDosagem = findViewById(R.id.campo_dosagem);
        campoOrientacoes = findViewById(R.id.campo_orientacoes);
        campoIntervalo = findViewById(R.id.campo_intervalo);
        campoInicio = findViewById(R.id.campo_inicio);
        campoFim = findViewById(R.id.campo_fim);
        nome = findViewById(R.id.entrada_nome);
        dosagem = findViewById(R.id.entrada_dosagem);
        orientacoes = findViewById(R.id.entrada_orientacoes);
        intervalo = findViewById(R.id.entrada_intervalo);
        entradaInicio = findViewById(R.id.entrada_inicio);
        entradaFim = findViewById(R.id.entrada_fim);
        chipsHorarios = findViewById(R.id.chips_horarios);
        chipsDias = findViewById(R.id.chips_dias);
        chipsTolerancia = findViewById(R.id.chips_tolerancia);
        botaoAddHorario = findViewById(R.id.botao_add_horario);

        chipDia.put(R.id.chip_dom, DayOfWeek.SUNDAY);
        chipDia.put(R.id.chip_seg, DayOfWeek.MONDAY);
        chipDia.put(R.id.chip_ter, DayOfWeek.TUESDAY);
        chipDia.put(R.id.chip_qua, DayOfWeek.WEDNESDAY);
        chipDia.put(R.id.chip_qui, DayOfWeek.THURSDAY);
        chipDia.put(R.id.chip_sex, DayOfWeek.FRIDAY);
        chipDia.put(R.id.chip_sab, DayOfWeek.SATURDAY);

        MaterialButtonToggleGroup grupo = findViewById(R.id.grupo_frequencia);
        grupo.check(R.id.botao_freq_fixos);
        grupo.addOnButtonCheckedListener((g, id, marcado) -> {
            if (!marcado) return;
            if (id == R.id.botao_freq_intervalo) frequencia = TipoFrequencia.A_CADA_N_HORAS;
            else if (id == R.id.botao_freq_dias) frequencia = TipoFrequencia.DIAS_SEMANA;
            else frequencia = TipoFrequencia.DIARIA;
            atualizarFrequencia();
        });

        chipsTolerancia.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_tol_15) aplicarTolerancia(15);
            else if (id == R.id.chip_tol_30) aplicarTolerancia(30);
            else if (id == R.id.chip_tol_60) aplicarTolerancia(60);
        });
        findViewById(R.id.chip_tol_outro).setOnClickListener(v -> pedirToleranciaPersonalizada());

        botaoAddHorario.setOnClickListener(v -> editarHorario(-1));
        entradaInicio.setOnClickListener(v -> escolherData(true));
        entradaFim.setOnClickListener(v -> escolherData(false));
        campoFim.setEndIconOnClickListener(v -> {
            dataFim = null;
            entradaFim.setText("");
        });
        findViewById(R.id.botao_salvar).setOnClickListener(v -> salvar());

        entradaInicio.setText(Formatos.data(dataInicio));
        atualizarFrequencia();
        if (medicamentoId != null) {
            carregar();
        } else {
            Horario h = new Horario();
            h.hora = "08:00";
            h.toleranciaMinutos = toleranciaPadrao;
            horarios.add(h);
            desenharHorarios();
        }
    }

    private void carregar() {
        View progresso = findViewById(R.id.progresso);
        progresso.setVisibility(View.VISIBLE);
        Repositorios.get(this).buscarMedicamento(medicamentoId, new Callback<Medicamento>() {
            @Override
            public void sucesso(Medicamento m) {
                progresso.setVisibility(View.GONE);
                preencher(m);
            }

            @Override
            public void erro(ErroApi erro) {
                progresso.setVisibility(View.GONE);
                Ui.erro(findViewById(R.id.raiz), erro);
            }
        });
    }

    private void preencher(Medicamento m) {
        nome.setText(m.nome);
        dosagem.setText(m.dosagem);
        orientacoes.setText(m.orientacoes);
        dataInicio = m.dataInicio;
        dataFim = m.dataFim;
        entradaInicio.setText(Formatos.data(dataInicio));
        entradaFim.setText(dataFim == null ? "" : Formatos.data(dataFim));
        horarios.clear();
        horarios.addAll(m.horarios);
        if (!horarios.isEmpty()) {
            Horario primeiro = horarios.get(0);
            frequencia = TipoFrequencia.valueOf(primeiro.tipoFrequencia);
            MaterialButtonToggleGroup grupo = findViewById(R.id.grupo_frequencia);
            grupo.check(frequencia == TipoFrequencia.A_CADA_N_HORAS ? R.id.botao_freq_intervalo
                    : frequencia == TipoFrequencia.DIAS_SEMANA ? R.id.botao_freq_dias : R.id.botao_freq_fixos);
            if (primeiro.intervaloHoras != null) {
                intervalo.setText(String.valueOf(primeiro.intervaloHoras));
            }
            for (Map.Entry<Integer, DayOfWeek> e : chipDia.entrySet()) {
                ((Chip) findViewById(e.getKey())).setChecked(primeiro.diasSemana != null
                        && primeiro.diasSemana.contains(e.getValue().name()));
            }
            toleranciaPadrao = primeiro.toleranciaMinutos == null ? 30 : primeiro.toleranciaMinutos;
            marcarChipTolerancia(toleranciaPadrao);
        }
        atualizarFrequencia();
        desenharHorarios();
    }

    private void atualizarFrequencia() {
        campoIntervalo.setVisibility(frequencia == TipoFrequencia.A_CADA_N_HORAS ? View.VISIBLE : View.GONE);
        findViewById(R.id.grupo_dias).setVisibility(frequencia == TipoFrequencia.DIAS_SEMANA ? View.VISIBLE : View.GONE);
        ((android.widget.TextView) findViewById(R.id.titulo_horarios)).setText(
                frequencia == TipoFrequencia.A_CADA_N_HORAS ? R.string.primeiro_horario : R.string.horarios);
        if (frequencia == TipoFrequencia.A_CADA_N_HORAS && horarios.size() > 1) {
            Horario primeiro = horarios.get(0);
            horarios.clear();
            horarios.add(primeiro);
        }
        desenharHorarios();
    }

    private void desenharHorarios() {
        chipsHorarios.removeAllViews();
        for (int i = 0; i < horarios.size(); i++) {
            Horario h = horarios.get(i);
            final int indice = i;
            Chip chip = new Chip(this);
            chip.setText(getString(R.string.horario_chip, h.hora, h.toleranciaMinutos == null ? 30 : h.toleranciaMinutos));
            chip.setTextSize(16);
            chip.setChipMinHeight(getResources().getDimension(R.dimen.toque_minimo));
            chip.setCloseIconVisible(horarios.size() > 1);
            chip.setCloseIconContentDescription(getString(R.string.remover_horario, h.hora));
            chip.setContentDescription(getString(R.string.editar_horario_desc, h.hora));
            chip.setOnClickListener(v -> editarHorario(indice));
            chip.setOnCloseIconClickListener(v -> {
                horarios.remove(indice);
                desenharHorarios();
            });
            chipsHorarios.addView(chip);
        }
        botaoAddHorario.setVisibility(frequencia == TipoFrequencia.A_CADA_N_HORAS && !horarios.isEmpty()
                ? View.GONE : View.VISIBLE);
    }

    private void editarHorario(int indice) {
        Horario atual = indice >= 0 ? horarios.get(indice) : null;
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_horario, null);
        MaterialButton botaoHora = v.findViewById(R.id.botao_hora);
        EditText tolerancia = v.findViewById(R.id.entrada_tolerancia);
        TextInputLayout campoTolerancia = v.findViewById(R.id.campo_tolerancia);
        final String[] hora = {atual != null ? atual.hora : "08:00"};
        botaoHora.setText(hora[0]);
        tolerancia.setText(String.valueOf(atual != null && atual.toleranciaMinutos != null
                ? atual.toleranciaMinutos : toleranciaPadrao));
        botaoHora.setOnClickListener(b -> {
            LocalTime t = ValidadorTratamento.parseHora(hora[0]);
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(t.getHour())
                    .setMinute(t.getMinute())
                    .setTitleText(R.string.escolha_horario)
                    .build();
            picker.addOnPositiveButtonClickListener(p -> {
                hora[0] = String.format(Locale.ROOT, "%02d:%02d", picker.getHour(), picker.getMinute());
                botaoHora.setText(hora[0]);
            });
            picker.show(getSupportFragmentManager(), "hora");
        });

        androidx.appcompat.app.AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(atual == null ? R.string.adicionar_horario : R.string.editar_horario)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {
                    Integer tol;
                    try {
                        tol = ValidadorTratamento.toleranciaOuPadrao(parseInteiro(Ui.texto(tolerancia)));
                    } catch (ValidacaoException e) {
                        campoTolerancia.setError(e.getMessage());
                        return;
                    }
                    for (int i = 0; i < horarios.size(); i++) {
                        if (i != indice && horarios.get(i).hora.equals(hora[0])) {
                            campoTolerancia.setError(getString(R.string.horario_repetido));
                            return;
                        }
                    }
                    Horario h = atual != null ? atual : new Horario();
                    h.hora = hora[0];
                    h.toleranciaMinutos = tol;
                    if (atual == null) {
                        horarios.add(h);
                    }
                    horarios.sort((a, c) -> a.hora.compareTo(c.hora));
                    desenharHorarios();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }

    private void aplicarTolerancia(int minutos) {
        toleranciaPadrao = minutos;
        for (Horario h : horarios) {
            h.toleranciaMinutos = minutos;
        }
        desenharHorarios();
    }

    private void marcarChipTolerancia(int minutos) {
        int id = minutos == 15 ? R.id.chip_tol_15 : minutos == 30 ? R.id.chip_tol_30
                : minutos == 60 ? R.id.chip_tol_60 : R.id.chip_tol_outro;
        chipsTolerancia.check(id);
        if (id == R.id.chip_tol_outro) {
            ((Chip) findViewById(R.id.chip_tol_outro)).setText(getString(R.string.tol_minutos, minutos));
        }
    }

    private void pedirToleranciaPersonalizada() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_texto, null);
        TextInputLayout campo = v.findViewById(R.id.campo_texto);
        EditText entrada = v.findViewById(R.id.entrada_texto);
        campo.setHint(R.string.tolerancia_minutos);
        campo.setHelperText(getString(R.string.tolerancia_intervalo));
        entrada.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        entrada.setText(String.valueOf(toleranciaPadrao));
        androidx.appcompat.app.AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tolerancia_confirmar)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancelar, (d, w) -> marcarChipTolerancia(toleranciaPadrao))
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {
                    try {
                        int tol = ValidadorTratamento.toleranciaOuPadrao(parseInteiro(Ui.texto(entrada)));
                        aplicarTolerancia(tol);
                        marcarChipTolerancia(tol);
                        dialogo.dismiss();
                    } catch (ValidacaoException e) {
                        campo.setError(e.getMessage());
                    }
                }));
        dialogo.show();
    }

    private void escolherData(boolean inicio) {
        LocalDate atual = inicio ? dataInicio : (dataFim != null ? dataFim : dataInicio);
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(inicio ? R.string.inicio : R.string.termino_opcional)
                .setSelection(atual.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
        picker.addOnPositiveButtonClickListener(ms -> {
            LocalDate d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate();
            if (inicio) {
                dataInicio = d;
                entradaInicio.setText(Formatos.data(d));
            } else {
                dataFim = d;
                entradaFim.setText(Formatos.data(d));
            }
            campoInicio.setError(null);
            campoFim.setError(null);
        });
        picker.show(getSupportFragmentManager(), "data");
    }

    private static Integer parseInteiro(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private MedicamentoRequest montar() {
        MedicamentoRequest req = new MedicamentoRequest();
        req.nome = Ui.texto(nome);
        req.dosagem = Ui.texto(dosagem);
        String o = Ui.texto(orientacoes);
        req.orientacoes = o.isEmpty() ? null : o;
        req.dataInicio = dataInicio;
        req.dataFim = dataFim;
        ValidadorTratamento.validarMedicamento(req.nome, req.dosagem, req.dataInicio, req.dataFim);
        if (horarios.isEmpty()) {
            throw new ValidacaoException("horarios", getString(R.string.erro_sem_horario));
        }
        Integer n = frequencia == TipoFrequencia.A_CADA_N_HORAS ? parseInteiro(Ui.texto(intervalo)) : null;
        List<String> dias = new ArrayList<>();
        EnumSet<DayOfWeek> conjunto = EnumSet.noneOf(DayOfWeek.class);
        if (frequencia == TipoFrequencia.DIAS_SEMANA) {
            for (Map.Entry<Integer, DayOfWeek> e : chipDia.entrySet()) {
                if (((Chip) findViewById(e.getKey())).isChecked()) {
                    dias.add(e.getValue().name());
                    conjunto.add(e.getValue());
                }
            }
        }
        for (Horario h : horarios) {
            Horario novo = new Horario();
            novo.hora = h.hora;
            novo.tipoFrequencia = frequencia.name();
            novo.intervaloHoras = n;
            novo.diasSemana = new ArrayList<>(dias);
            novo.toleranciaMinutos = ValidadorTratamento.toleranciaOuPadrao(h.toleranciaMinutos);
            ValidadorTratamento.validarHorario(new RegraHorario(ValidadorTratamento.parseHora(novo.hora),
                    frequencia, n, conjunto, novo.toleranciaMinutos));
            req.horarios.add(novo);
        }
        return req;
    }

    private void salvar() {
        Ui.limparErros(campoNome, campoDosagem, campoOrientacoes, campoIntervalo, campoInicio, campoFim);
        MedicamentoRequest req;
        try {
            req = montar();
        } catch (ValidacaoException e) {
            mostrarErro(new ErroApi(e.getMessage(), e.getCampo()));
            return;
        }
        Ui.esconderTeclado(this);
        View botao = findViewById(R.id.botao_salvar);
        View progresso = findViewById(R.id.progresso);
        Ui.carregando(botao, progresso, true);
        Repositorios.get(this).salvarMedicamento(idosoId, medicamentoId, req, new Callback<Medicamento>() {
            @Override
            public void sucesso(Medicamento m) {
                finish();
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.carregando(botao, progresso, false);
                mostrarErro(erro);
            }
        });
    }

    private void mostrarErro(ErroApi erro) {
        TextInputLayout campo = null;
        if (erro.campo != null) {
            switch (erro.campo) {
                case "nome": campo = campoNome; break;
                case "dosagem": campo = campoDosagem; break;
                case "orientacoes": campo = campoOrientacoes; break;
                case "dataInicio": campo = campoInicio; break;
                case "dataFim": campo = campoFim; break;
                case "intervaloHoras": campo = campoIntervalo; break;
                default: break;
            }
        }
        Ui.erroNoCampo(findViewById(R.id.raiz), erro, campo);
    }
}
