package br.ucsal.medroutine.model;

import br.ucsal.medroutine.domain.EstadoDose;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "dose_programada")
public class DoseProgramada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id")
    private HorarioMedicacao horario;

    @Column(name = "data_hora_prevista", nullable = false)
    private Instant dataHoraPrevista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoDose estado = EstadoDose.PENDENTE;

    protected DoseProgramada() { }

    public DoseProgramada(HorarioMedicacao horario, Instant dataHoraPrevista) {
        this.horario = horario;
        this.dataHoraPrevista = dataHoraPrevista;
    }

    public Medicamento getMedicamento() {
        return horario.getMedicamento();
    }

    public Usuario getIdoso() {
        return horario.getMedicamento().getIdoso();
    }

    public Long getId() { return id; }
    public HorarioMedicacao getHorario() { return horario; }
    public Instant getDataHoraPrevista() { return dataHoraPrevista; }
    public EstadoDose getEstado() { return estado; }
    public void setEstado(EstadoDose estado) { this.estado = estado; }
}
