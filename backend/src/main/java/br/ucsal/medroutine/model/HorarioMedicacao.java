package br.ucsal.medroutine.model;

import br.ucsal.medroutine.domain.RegraHorario;
import br.ucsal.medroutine.domain.TipoFrequencia;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "horario_medicacao")
public class HorarioMedicacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id")
    private Medicamento medicamento;

    @Column(nullable = false)
    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_frequencia", nullable = false, length = 20)
    private TipoFrequencia tipoFrequencia;

    @Column(name = "intervalo_horas")
    private Integer intervaloHoras;

    @Column(name = "dias_semana", length = 70)
    private String diasSemana;

    @Column(name = "tolerancia_minutos", nullable = false)
    private int toleranciaMinutos;

    @Column(nullable = false)
    private boolean ativo = true;

    protected HorarioMedicacao() { }

    public HorarioMedicacao(Medicamento medicamento, RegraHorario regra) {
        this.medicamento = medicamento;
        this.hora = regra.getHora();
        this.tipoFrequencia = regra.getTipoFrequencia();
        this.intervaloHoras = regra.getTipoFrequencia() == TipoFrequencia.A_CADA_N_HORAS
                ? regra.getIntervaloHoras() : null;
        this.diasSemana = regra.getTipoFrequencia() == TipoFrequencia.DIAS_SEMANA
                ? regra.getDiasSemana().stream().map(Enum::name).collect(Collectors.joining(",")) : null;
        this.toleranciaMinutos = regra.getToleranciaMinutos();
    }

    public RegraHorario toRegra() {
        return new RegraHorario(hora, tipoFrequencia, intervaloHoras, getDiasSemanaSet(), toleranciaMinutos);
    }

    public Set<DayOfWeek> getDiasSemanaSet() {
        if (diasSemana == null || diasSemana.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(diasSemana.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public Long getId() { return id; }
    public Medicamento getMedicamento() { return medicamento; }
    public LocalTime getHora() { return hora; }
    public TipoFrequencia getTipoFrequencia() { return tipoFrequencia; }
    public Integer getIntervaloHoras() { return intervaloHoras; }
    public int getToleranciaMinutos() { return toleranciaMinutos; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
