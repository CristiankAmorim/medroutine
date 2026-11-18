package br.ucsal.medroutine.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "confirmacao_dose")
public class ConfirmacaoDose {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dose_id", unique = true)
    private DoseProgramada dose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "pelo_cuidador", nullable = false)
    private boolean peloCuidador;

    @Column(name = "confirmado_em", nullable = false)
    private Instant confirmadoEm;

    @Column(name = "horario_aparelho")
    private Instant horarioAparelho;

    protected ConfirmacaoDose() { }

    public ConfirmacaoDose(DoseProgramada dose, Usuario usuario, Instant confirmadoEm, Instant horarioAparelho) {
        this.dose = dose;
        this.usuario = usuario;
        this.peloCuidador = usuario.isCuidador();
        this.confirmadoEm = confirmadoEm;
        this.horarioAparelho = horarioAparelho;
    }

    public Long getId() { return id; }
    public DoseProgramada getDose() { return dose; }
    public Usuario getUsuario() { return usuario; }
    public boolean isPeloCuidador() { return peloCuidador; }
    public Instant getConfirmadoEm() { return confirmadoEm; }
    public Instant getHorarioAparelho() { return horarioAparelho; }
}
