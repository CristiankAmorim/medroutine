package br.ucsal.medroutine.demo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.model.Agenda;
import br.ucsal.medroutine.data.model.Codigo;
import br.ucsal.medroutine.data.model.Confirmacao;
import br.ucsal.medroutine.data.model.ConfirmacaoResponse;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Horario;
import br.ucsal.medroutine.data.model.Idoso;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.data.model.MedicamentoRequest;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.data.model.NovoIdosoResponse;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.data.model.Usuario;
import br.ucsal.medroutine.data.remote.GsonFactory;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.MedRoutineRepository;
import br.ucsal.medroutine.domain.DoseJaConfirmadaException;
import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.domain.GeradorDoses;
import br.ucsal.medroutine.domain.RegraHorario;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.domain.TipoFrequencia;
import br.ucsal.medroutine.domain.ValidacaoException;
import br.ucsal.medroutine.domain.ValidadorTratamento;
import br.ucsal.medroutine.notification.Notificador;
import br.ucsal.medroutine.util.Executores;

public class DemoRepository implements MedRoutineRepository {
    public static final String TOKEN_DEMO = "demo";
    private static final long ID_CUIDADOR = 1;
    private static final long ID_IDOSO = 2;
    private static final String INDISPONIVEL = "Esta função não está disponível no modo demonstração.";

    private static DemoRepository instancia;

    private final Context app;
    private final File arquivo;
    private final Gson gson = GsonFactory.get();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SecureRandom random = new SecureRandom();
    private Dados dados;

    static class Dados {
        long proximoId = 100;
        Usuario cuidador;
        List<Idoso> idosos = new ArrayList<>();
        List<Medicamento> medicamentos = new ArrayList<>();
        List<DoseDemo> doses = new ArrayList<>();
        List<Notificacao> notificacoes = new ArrayList<>();
    }

    private DemoRepository(Context context) {
        this.app = context.getApplicationContext();
        this.arquivo = new File(app.getFilesDir(), "demonstracao.json");
    }

    public static synchronized DemoRepository get(Context context) {
        if (instancia == null) {
            instancia = new DemoRepository(context);
        }
        return instancia;
    }

    public TokenResponse iniciar(boolean comoIdoso) {
        synchronized (this) {
            garantir();
            salvar();
        }
        agendarVerificacoes();
        Usuario usuario;
        if (comoIdoso) {
            usuario = usuarioIdoso();
        } else {
            usuario = copia(dados.cuidador, Usuario.class);
        }
        return new TokenResponse(TOKEN_DEMO, usuario);
    }

    public synchronized void reiniciar() {
        arquivo.delete();
        dados = null;
        garantir();
        salvar();
        agendarVerificacoes();
    }

    private void garantir() {
        if (dados == null) {
            dados = carregar();
            if (dados == null) {
                dados = semear();
            }
        }
        renovarHorizonte();
    }

    private Dados carregar() {
        if (!arquivo.exists()) {
            return null;
        }
        try (Reader r = new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8)) {
            return gson.fromJson(r, Dados.class);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private void salvar() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8)) {
            gson.toJson(dados, w);
        } catch (IOException ignorado) {
        }
    }

    private long novoId() {
        return dados.proximoId++;
    }

    private ZoneId zona() {
        return ZoneId.systemDefault();
    }

    private Dados semear() {
        dados = new Dados();
        Usuario cuidador = new Usuario();
        cuidador.id = ID_CUIDADOR;
        cuidador.nome = "Ana Souza";
        cuidador.email = "ana@demonstracao.medroutine";
        cuidador.tipo = Usuario.CUIDADOR;
        dados.cuidador = cuidador;

        Idoso maria = new Idoso();
        maria.id = ID_IDOSO;
        maria.nome = "Maria Souza";
        maria.fusoHorario = zona().getId();
        maria.anuncioVoz = true;
        maria.totalCuidadores = 1;
        dados.idosos.add(maria);

        LocalDate hoje = LocalDate.now(zona());
        LocalDate inicio = hoje.minusDays(7);
        Instant agora = Instant.now();
        LocalTime rapida = LocalTime.now(zona()).plusMinutes(2).truncatedTo(ChronoUnit.MINUTES);

        criarMedicamento(ID_IDOSO, "Levotiroxina", "50 mcg", "Em jejum, 30 minutos antes do café.", inicio,
                new String[]{"07:00"}, 30);
        criarMedicamento(ID_IDOSO, "Losartana", "50 mg", "1 comprimido, depois do café da manhã.", inicio,
                new String[]{"08:00", "20:00"}, 30);
        criarMedicamento(ID_IDOSO, "Metformina", "850 mg", "1 comprimido, no almoço.", inicio,
                new String[]{"12:00"}, 30);
        criarMedicamento(ID_IDOSO, "Vitamina D", "1.000 UI",
                "Dose de demonstração rápida: o lembrete toca em 2 minutos e o alerta ao cuidador sai 5 minutos depois.",
                hoje, new String[]{String.format(java.util.Locale.ROOT, "%02d:%02d", rapida.getHour(), rapida.getMinute())}, 5);

        Instant de = inicio.atStartOfDay(zona()).toInstant();
        for (Medicamento m : dados.medicamentos) {
            for (Horario h : m.horarios) {
                for (Instant previsto : GeradorDoses.gerar(regra(h), m.dataInicio, m.dataFim, zona(), de,
                        agora.plus(Duration.ofDays(GeradorDoses.HORIZONTE_DIAS)))) {
                    DoseDemo d = novaDose(m, h, previsto);
                    simularPassado(d, agora);
                    dados.doses.add(d);
                }
            }
        }
        ordenarDoses();
        return dados;
    }

    private void simularPassado(DoseDemo d, Instant agora) {
        Instant limite = RegrasDose.limiteTolerancia(d.dataHoraPrevista, d.toleranciaMinutos);
        if (!agora.isAfter(limite)) {
            return;
        }
        int i = (int) (d.id % 11);
        if (i == 5) {
            d.estado = EstadoDose.NAO_CONFIRMADA.name();
            registrarNotificacao(d, Notificacao.ALERTA, limite.plusSeconds(40));
        } else if (i == 8) {
            registrarNotificacao(d, Notificacao.ALERTA, limite.plusSeconds(40));
            Instant quando = limite.plus(Duration.ofMinutes(22));
            d.estado = EstadoDose.CONFIRMADA_COM_ATRASO.name();
            d.confirmacao = confirmacao(quando, nomeIdoso(d.idosoId), false);
            registrarNotificacao(d, Notificacao.CONFIRMACAO, quando);
        } else {
            Instant quando = d.dataHoraPrevista.plus(Duration.ofMinutes(2 + (d.id % 4) * 3));
            d.estado = EstadoDose.CONFIRMADA.name();
            d.confirmacao = confirmacao(quando, nomeIdoso(d.idosoId), false);
            registrarNotificacao(d, Notificacao.CONFIRMACAO, quando);
        }
    }

    private void criarMedicamento(long idosoId, String nome, String dosagem, String orientacoes, LocalDate inicio,
                                  String[] horas, int tolerancia) {
        Medicamento m = new Medicamento();
        m.id = novoId();
        m.idosoId = idosoId;
        m.nome = nome;
        m.dosagem = dosagem;
        m.orientacoes = orientacoes;
        m.dataInicio = inicio;
        for (String hora : horas) {
            Horario h = new Horario();
            h.id = novoId();
            h.hora = hora;
            h.tipoFrequencia = TipoFrequencia.DIARIA.name();
            h.toleranciaMinutos = tolerancia;
            m.horarios.add(h);
        }
        dados.medicamentos.add(m);
    }

    private static RegraHorario regra(Horario h) {
        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        if (h.diasSemana != null) {
            for (String d : h.diasSemana) {
                dias.add(DayOfWeek.valueOf(d));
            }
        }
        return new RegraHorario(ValidadorTratamento.parseHora(h.hora), TipoFrequencia.valueOf(h.tipoFrequencia),
                h.intervaloHoras, dias, ValidadorTratamento.toleranciaOuPadrao(h.toleranciaMinutos));
    }

    private DoseDemo novaDose(Medicamento m, Horario h, Instant previsto) {
        DoseDemo d = new DoseDemo();
        d.id = novoId();
        d.horarioId = h.id;
        d.idosoId = m.idosoId;
        d.idosoNome = nomeIdoso(m.idosoId);
        d.medicamentoId = m.id;
        d.medicamentoNome = m.nome;
        d.dosagem = m.dosagem;
        d.orientacoes = m.orientacoes;
        d.dataHoraPrevista = previsto;
        d.toleranciaMinutos = h.toleranciaMinutos == null ? ValidadorTratamento.TOLERANCIA_PADRAO : h.toleranciaMinutos;
        d.estado = EstadoDose.PENDENTE.name();
        return d;
    }

    private void renovarHorizonte() {
        Instant agora = Instant.now();
        boolean mudou = false;
        for (Medicamento m : dados.medicamentos) {
            if (!m.ativo) {
                continue;
            }
            for (Horario h : m.horarios) {
                for (Instant previsto : GeradorDoses.gerarHorizonte(regra(h), m.dataInicio, m.dataFim, zona(),
                        () -> agora)) {
                    if (!existeDose(h.id, previsto)) {
                        dados.doses.add(novaDose(m, h, previsto));
                        mudou = true;
                    }
                }
            }
        }
        if (mudou) {
            ordenarDoses();
        }
    }

    private boolean existeDose(long horarioId, Instant previsto) {
        for (DoseDemo d : dados.doses) {
            if (d.horarioId == horarioId && d.dataHoraPrevista.equals(previsto)) {
                return true;
            }
        }
        return false;
    }

    private void ordenarDoses() {
        dados.doses.sort((a, b) -> a.dataHoraPrevista.compareTo(b.dataHoraPrevista));
    }

    private void verificarTolerancias() {
        Instant agora = Instant.now();
        for (DoseDemo d : dados.doses) {
            if (RegrasDose.deveAlertar(d.estadoDose(), d.dataHoraPrevista, d.toleranciaMinutos, agora)) {
                d.estado = EstadoDose.NAO_CONFIRMADA.name();
                registrarNotificacao(d, Notificacao.ALERTA, agora);
                Notificador.mostrarParaCuidador(app, Notificacao.ALERTA, d.id, d.idosoNome,
                        d.medicamentoNome + " " + d.dosagem, d.dataHoraPrevista, d.toleranciaMinutos, null, false);
            }
        }
    }

    public void verificarAgora() {
        synchronized (this) {
            garantir();
            verificarTolerancias();
            salvar();
        }
        agendarVerificacoes();
    }

    public void agendarVerificacoes() {
        Instant proximo = null;
        synchronized (this) {
            garantir();
            Instant agora = Instant.now();
            for (DoseDemo d : dados.doses) {
                if (EstadoDose.PENDENTE.name().equals(d.estado)) {
                    Instant limite = RegrasDose.limiteTolerancia(d.dataHoraPrevista, d.toleranciaMinutos)
                            .plusSeconds(2);
                    if (limite.isAfter(agora) && (proximo == null || limite.isBefore(proximo))) {
                        proximo = limite;
                    }
                }
            }
        }
        AlarmManager am = app.getSystemService(AlarmManager.class);
        if (am == null) {
            return;
        }
        PendingIntent pi = PendingIntent.getBroadcast(app, 9001, new Intent(app, DemoVerificacaoReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (proximo == null) {
            return;
        }
        try {
            if (AgendadorLembretes.podeAgendarExato(app)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo.toEpochMilli(), pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo.toEpochMilli(), pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo.toEpochMilli(), pi);
        }
    }

    public void confirmarComoIdoso(long doseId, Instant agora) {
        synchronized (this) {
            garantir();
            DoseDemo d = buscarDose(doseId);
            if (d == null || d.estadoDose().isConfirmada()) {
                return;
            }
            try {
                registrarConfirmacao(d, agora, false);
            } catch (ValidacaoException | DoseJaConfirmadaException e) {
                return;
            }
            salvar();
        }
        agendarVerificacoes();
    }

    private void registrarConfirmacao(DoseDemo d, Instant agora, boolean peloCuidador) {
        EstadoDose novo = RegrasDose.estadoAposConfirmacao(d.estadoDose(), d.dataHoraPrevista, d.toleranciaMinutos,
                agora);
        d.estado = novo.name();
        d.confirmacao = confirmacao(agora, peloCuidador ? dados.cuidador.nome : nomeIdoso(d.idosoId), peloCuidador);
        if (!peloCuidador) {
            registrarNotificacao(d, Notificacao.CONFIRMACAO, agora);
            Notificador.mostrarParaCuidador(app, Notificacao.CONFIRMACAO, d.id, d.idosoNome,
                    d.medicamentoNome + " " + d.dosagem, d.dataHoraPrevista, d.toleranciaMinutos, agora,
                    novo == EstadoDose.CONFIRMADA_COM_ATRASO);
        }
    }

    private Confirmacao confirmacao(Instant quando, String quem, boolean peloCuidador) {
        Confirmacao c = new Confirmacao();
        c.confirmadoEm = quando;
        c.horarioAparelho = quando;
        c.confirmadoPor = quem;
        c.peloCuidador = peloCuidador;
        return c;
    }

    private void registrarNotificacao(DoseDemo d, String tipo, Instant quando) {
        Notificacao n = new Notificacao();
        n.id = novoId();
        n.doseId = d.id;
        n.tipo = tipo;
        n.estadoEnvio = "ENVIADA";
        n.tentativas = 1;
        n.criadoEm = quando;
        n.enviadoEm = quando;
        n.destinatario = dados.cuidador.nome;
        n.idosoId = d.idosoId;
        n.idosoNome = d.idosoNome;
        n.medicamentoNome = d.medicamentoNome;
        n.dosagem = d.dosagem;
        n.dataHoraPrevista = d.dataHoraPrevista;
        n.toleranciaMinutos = d.toleranciaMinutos;
        dados.notificacoes.add(n);
    }

    private DoseDemo buscarDose(long id) {
        for (DoseDemo d : dados.doses) {
            if (d.id == id) {
                return d;
            }
        }
        return null;
    }

    private Medicamento buscarMedicamento(long id) {
        for (Medicamento m : dados.medicamentos) {
            if (m.id == id) {
                return m;
            }
        }
        throw new IllegalArgumentException("Medicamento não encontrado.");
    }

    private Idoso buscarIdoso(long id) {
        for (Idoso i : dados.idosos) {
            if (i.id == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("Idoso não encontrado.");
    }

    private String nomeIdoso(long id) {
        for (Idoso i : dados.idosos) {
            if (i.id == id) {
                return i.nome;
            }
        }
        return "";
    }

    private Usuario usuarioIdoso() {
        Idoso i = buscarIdoso(ID_IDOSO);
        Usuario u = new Usuario();
        u.id = i.id;
        u.nome = i.nome;
        u.tipo = Usuario.IDOSO;
        u.fusoHorario = i.fusoHorario;
        u.anuncioVoz = i.anuncioVoz;
        return u;
    }

    private <T> T copia(Object origem, Type tipo) {
        return gson.fromJson(gson.toJson(origem), tipo);
    }

    private List<Dose> copiaDoses(List<? extends Dose> doses, boolean comNotificacoes) {
        List<Dose> lista = copia(doses, new TypeToken<List<Dose>>() { }.getType());
        for (Dose d : lista) {
            d.notificacoes = new ArrayList<>();
            if (comNotificacoes) {
                for (Notificacao n : dados.notificacoes) {
                    if (n.doseId == d.id) {
                        Notificacao c = copia(n, Notificacao.class);
                        c.estadoDose = d.estado;
                        d.notificacoes.add(c);
                    }
                }
            }
        }
        return lista;
    }

    private Codigo codigoAleatorio(int digitos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos; i++) {
            sb.append(random.nextInt(10));
        }
        return new Codigo(sb.toString(), Instant.now().plus(Duration.ofHours(24)));
    }

    private <T> void rodar(Supplier<T> operacao, Callback<T> cb) {
        executor.execute(() -> {
            T resultado;
            try {
                synchronized (this) {
                    garantir();
                    verificarTolerancias();
                    resultado = operacao.get();
                    salvar();
                }
            } catch (ValidacaoException e) {
                Executores.principal(() -> cb.erro(new ErroApi(e.getMessage(), e.getCampo())));
                return;
            } catch (RuntimeException e) {
                Executores.principal(() -> cb.erro(new ErroApi(
                        e.getMessage() == null ? "Erro na demonstração." : e.getMessage(), null)));
                return;
            }
            agendarVerificacoes();
            final T pronto = resultado;
            Executores.principal(() -> cb.sucesso(pronto));
        });
    }

    private <T> void indisponivel(Callback<T> cb) {
        Executores.principal(() -> cb.erro(new ErroApi(INDISPONIVEL, null)));
    }

    @Override
    public void login(String email, String senha, Callback<TokenResponse> cb) {
        rodar(() -> new TokenResponse(TOKEN_DEMO, copia(dados.cuidador, Usuario.class)), cb);
    }

    @Override
    public void cadastrar(String nome, String email, String senha, boolean consentimento, Callback<TokenResponse> cb) {
        indisponivel(cb);
    }

    @Override
    public void recuperarSenha(String email, Callback<Void> cb) {
        indisponivel(cb);
    }

    @Override
    public void redefinirSenha(String email, String codigo, String novaSenha, Callback<Void> cb) {
        indisponivel(cb);
    }

    @Override
    public void entrarComCodigo(String codigo, Callback<TokenResponse> cb) {
        rodar(() -> new TokenResponse(TOKEN_DEMO, usuarioIdoso()), cb);
    }

    @Override
    public void registrarTokenAparelho(String token, Callback<Void> cb) {
        Executores.principal(() -> cb.sucesso(null));
    }

    @Override
    public void sair(Callback<Void> cb) {
        Executores.principal(() -> cb.sucesso(null));
    }

    @Override
    public void excluirConta(Callback<Void> cb) {
        executor.execute(() -> {
            reiniciar();
            Executores.principal(() -> cb.sucesso(null));
        });
    }

    @Override
    public void listarIdosos(Callback<List<Idoso>> cb) {
        rodar(() -> copia(dados.idosos, new TypeToken<List<Idoso>>() { }.getType()), cb);
    }

    @Override
    public void criarIdoso(String nome, boolean anuncioVoz, Callback<NovoIdosoResponse> cb) {
        rodar(() -> {
            if (nome == null || nome.trim().isEmpty()) {
                throw new ValidacaoException("nome", "Informe o nome do idoso.");
            }
            Idoso i = new Idoso();
            i.id = novoId();
            i.nome = nome.trim();
            i.fusoHorario = zona().getId();
            i.anuncioVoz = anuncioVoz;
            i.totalCuidadores = 1;
            dados.idosos.add(i);
            NovoIdosoResponse r = new NovoIdosoResponse();
            r.idoso = copia(i, Idoso.class);
            r.codigo = codigoAleatorio(6);
            return r;
        }, cb);
    }

    @Override
    public void atualizarIdoso(long idosoId, String nome, boolean anuncioVoz, Callback<Idoso> cb) {
        rodar(() -> {
            Idoso i = buscarIdoso(idosoId);
            if (nome == null || nome.trim().isEmpty()) {
                throw new ValidacaoException("nome", "Informe o nome do idoso.");
            }
            i.nome = nome.trim();
            i.anuncioVoz = anuncioVoz;
            for (DoseDemo d : dados.doses) {
                if (d.idosoId == idosoId) {
                    d.idosoNome = i.nome;
                }
            }
            return copia(i, Idoso.class);
        }, cb);
    }

    @Override
    public void excluirIdoso(long idosoId, Callback<Void> cb) {
        indisponivel(cb);
    }

    @Override
    public void gerarCodigoAcesso(long idosoId, Callback<Codigo> cb) {
        rodar(() -> codigoAleatorio(6), cb);
    }

    @Override
    public void gerarConvite(long idosoId, Callback<Codigo> cb) {
        rodar(() -> {
            Codigo c = codigoAleatorio(8);
            c.codigo = c.codigo.substring(0, 4) + "DEMO";
            return c;
        }, cb);
    }

    @Override
    public void aceitarConvite(String codigo, Callback<Idoso> cb) {
        indisponivel(cb);
    }

    @Override
    public void desvincular(long idosoId, Callback<Void> cb) {
        indisponivel(cb);
    }

    @Override
    public void listarMedicamentos(long idosoId, Callback<List<Medicamento>> cb) {
        rodar(() -> {
            List<Medicamento> lista = new ArrayList<>();
            for (Medicamento m : dados.medicamentos) {
                if (m.idosoId == idosoId) {
                    lista.add(m);
                }
            }
            List<Medicamento> c = copia(lista, new TypeToken<List<Medicamento>>() { }.getType());
            Collections.sort(c, (a, b) -> a.ativo != b.ativo ? (a.ativo ? -1 : 1) : a.nome.compareToIgnoreCase(b.nome));
            return c;
        }, cb);
    }

    @Override
    public void buscarMedicamento(long medicamentoId, Callback<Medicamento> cb) {
        rodar(() -> copia(buscarMedicamento(medicamentoId), Medicamento.class), cb);
    }

    @Override
    public void salvarMedicamento(long idosoId, Long medicamentoId, MedicamentoRequest req, Callback<Medicamento> cb) {
        rodar(() -> {
            ValidadorTratamento.validarMedicamento(req.nome, req.dosagem, req.dataInicio, req.dataFim);
            if (req.horarios == null || req.horarios.isEmpty()) {
                throw new ValidacaoException("horarios", "Adicione ao menos um horário.");
            }
            for (Horario h : req.horarios) {
                h.toleranciaMinutos = ValidadorTratamento.toleranciaOuPadrao(h.toleranciaMinutos);
                ValidadorTratamento.validarHorario(regra(h));
            }
            Medicamento m;
            Instant agora = Instant.now();
            if (medicamentoId == null) {
                m = new Medicamento();
                m.id = novoId();
                m.idosoId = idosoId;
                dados.medicamentos.add(m);
            } else {
                m = buscarMedicamento(medicamentoId);
                removerPendentesFuturas(m, agora);
            }
            m.nome = req.nome.trim();
            m.dosagem = req.dosagem.trim();
            m.orientacoes = req.orientacoes == null || req.orientacoes.trim().isEmpty() ? null : req.orientacoes.trim();
            m.dataInicio = req.dataInicio;
            m.dataFim = req.dataFim;
            m.ativo = true;
            m.horarios = new ArrayList<>();
            for (Horario h : req.horarios) {
                Horario novo = copia(h, Horario.class);
                novo.id = novoId();
                if (!TipoFrequencia.A_CADA_N_HORAS.name().equals(novo.tipoFrequencia)) {
                    novo.intervaloHoras = null;
                }
                if (!TipoFrequencia.DIAS_SEMANA.name().equals(novo.tipoFrequencia)) {
                    novo.diasSemana = new ArrayList<>();
                }
                m.horarios.add(novo);
            }
            for (DoseDemo d : dados.doses) {
                if (d.medicamentoId == m.id) {
                    d.medicamentoNome = m.nome;
                    d.dosagem = m.dosagem;
                    d.orientacoes = m.orientacoes;
                }
            }
            renovarHorizonte();
            return copia(m, Medicamento.class);
        }, cb);
    }

    private void removerPendentesFuturas(Medicamento m, Instant agora) {
        Iterator<DoseDemo> it = dados.doses.iterator();
        while (it.hasNext()) {
            DoseDemo d = it.next();
            if (d.medicamentoId == m.id && EstadoDose.PENDENTE.name().equals(d.estado)
                    && d.dataHoraPrevista.isAfter(agora)) {
                it.remove();
            }
        }
    }

    @Override
    public void desativarMedicamento(long medicamentoId, Callback<Medicamento> cb) {
        rodar(() -> {
            Medicamento m = buscarMedicamento(medicamentoId);
            m.ativo = false;
            removerPendentesFuturas(m, Instant.now());
            return copia(m, Medicamento.class);
        }, cb);
    }

    @Override
    public void dosesDoDia(long idosoId, LocalDate data, Callback<List<Dose>> cb) {
        rodar(() -> {
            Instant de = data.atStartOfDay(zona()).toInstant();
            Instant ate = data.plusDays(1).atStartOfDay(zona()).toInstant();
            List<DoseDemo> lista = new ArrayList<>();
            for (DoseDemo d : dados.doses) {
                if (d.idosoId == idosoId && !d.dataHoraPrevista.isBefore(de) && d.dataHoraPrevista.isBefore(ate)) {
                    lista.add(d);
                }
            }
            return copiaDoses(lista, false);
        }, cb);
    }

    @Override
    public void historico(long idosoId, LocalDate inicio, LocalDate fim, Long medicamentoId, Callback<List<Dose>> cb) {
        rodar(() -> {
            if (fim.isBefore(inicio)) {
                throw new ValidacaoException("fim", "A data final não pode ser anterior à inicial.");
            }
            Instant de = inicio.atStartOfDay(zona()).toInstant();
            Instant ate = fim.plusDays(1).atStartOfDay(zona()).toInstant();
            Instant agora = Instant.now();
            List<DoseDemo> lista = new ArrayList<>();
            for (DoseDemo d : dados.doses) {
                boolean noPeriodo = !d.dataHoraPrevista.isBefore(de) && d.dataHoraPrevista.isBefore(ate)
                        && d.dataHoraPrevista.isBefore(agora);
                if (d.idosoId == idosoId && noPeriodo && (medicamentoId == null || d.medicamentoId == medicamentoId)) {
                    lista.add(d);
                }
            }
            List<Dose> copia = copiaDoses(lista, true);
            Collections.reverse(copia);
            return copia;
        }, cb);
    }

    @Override
    public void notificacoes(Callback<List<Notificacao>> cb) {
        rodar(() -> {
            List<Notificacao> lista = copia(dados.notificacoes, new TypeToken<List<Notificacao>>() { }.getType());
            for (Notificacao n : lista) {
                DoseDemo d = buscarDose(n.doseId);
                n.estadoDose = d == null ? null : d.estado;
            }
            Collections.sort(lista, (a, b) -> b.criadoEm.compareTo(a.criadoEm));
            return lista.size() > 100 ? new ArrayList<Notificacao>(lista.subList(0, 100)) : lista;
        }, cb);
    }

    @Override
    public void dose(long doseId, Callback<Dose> cb) {
        rodar(() -> {
            DoseDemo d = buscarDose(doseId);
            if (d == null) {
                throw new IllegalArgumentException("Dose não encontrada.");
            }
            return copiaDoses(Collections.singletonList(d), true).get(0);
        }, cb);
    }

    @Override
    public void confirmarDose(long doseId, Callback<ConfirmacaoResponse> cb) {
        rodar(() -> {
            DoseDemo d = buscarDose(doseId);
            if (d == null) {
                throw new IllegalArgumentException("Dose não encontrada.");
            }
            ConfirmacaoResponse r = new ConfirmacaoResponse();
            if (d.estadoDose().isConfirmada()) {
                r.jaConfirmada = true;
            } else {
                registrarConfirmacao(d, Instant.now(), true);
            }
            r.dose = copiaDoses(Collections.singletonList(d), false).get(0);
            return r;
        }, cb);
    }

    @Override
    public void agenda(Callback<Agenda> cb) {
        rodar(() -> {
            Agenda a = new Agenda();
            a.idoso = usuarioIdoso();
            a.geradaEm = Instant.now();
            Instant de = LocalDate.now(zona()).atStartOfDay(zona()).toInstant();
            Instant ate = Instant.now().plus(Duration.ofDays(GeradorDoses.HORIZONTE_DIAS));
            List<DoseDemo> lista = new ArrayList<>();
            for (DoseDemo d : dados.doses) {
                if (d.idosoId == ID_IDOSO && !d.dataHoraPrevista.isBefore(de) && d.dataHoraPrevista.isBefore(ate)) {
                    lista.add(d);
                }
            }
            a.doses = copiaDoses(lista, false);
            return a;
        }, cb);
    }
}
