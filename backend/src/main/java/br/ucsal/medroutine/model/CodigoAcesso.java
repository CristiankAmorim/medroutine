package br.ucsal.medroutine.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "codigo_acesso")
public class CodigoAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCodigo tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected CodigoAcesso() { }

    public CodigoAcesso(String codigoHash, TipoCodigo tipo, Usuario usuario, Usuario criadoPor,
                        Instant criadoEm, Instant expiraEm) {
        this.codigoHash = codigoHash;
        this.tipo = tipo;
        this.usuario = usuario;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean isValido(Instant agora) {
        return usadoEm == null && agora.isBefore(expiraEm);
    }

    public void marcarUsado(Instant agora) {
        this.usadoEm = agora;
    }

    public Long getId() { return id; }
    public String getCodigoHash() { return codigoHash; }
    public TipoCodigo getTipo() { return tipo; }
    public Usuario getUsuario() { return usuario; }
    public Usuario getCriadoPor() { return criadoPor; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getUsadoEm() { return usadoEm; }
}
