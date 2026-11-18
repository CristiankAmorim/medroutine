package br.ucsal.medroutine.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notificacao")
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dose_id")
    private DoseProgramada dose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoNotificacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 10)
    private EstadoEnvio estadoEnvio = EstadoEnvio.PENDENTE;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    protected Notificacao() { }

    public Notificacao(DoseProgramada dose, Usuario destinatario, TipoNotificacao tipo, Instant criadoEm) {
        this.dose = dose;
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.criadoEm = criadoEm;
    }

    public void registrarTentativa(boolean sucesso, String erro, Instant quando) {
        tentativas++;
        if (sucesso) {
            estadoEnvio = EstadoEnvio.ENVIADA;
            enviadoEm = quando;
            ultimoErro = null;
        } else {
            ultimoErro = erro == null ? null : erro.substring(0, Math.min(erro.length(), 255));
        }
    }

    public void marcarFalha() {
        estadoEnvio = EstadoEnvio.FALHA;
    }

    public Long getId() { return id; }
    public DoseProgramada getDose() { return dose; }
    public Usuario getDestinatario() { return destinatario; }
    public TipoNotificacao getTipo() { return tipo; }
    public EstadoEnvio getEstadoEnvio() { return estadoEnvio; }
    public int getTentativas() { return tentativas; }
    public String getUltimoErro() { return ultimoErro; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getEnviadoEm() { return enviadoEm; }
}
