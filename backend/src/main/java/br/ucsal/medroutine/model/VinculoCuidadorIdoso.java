package br.ucsal.medroutine.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "vinculo_cuidador_idoso")
public class VinculoCuidadorIdoso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuidador_id")
    private Usuario cuidador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idoso_id")
    private Usuario idoso;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "desfeito_em")
    private Instant desfeitoEm;

    protected VinculoCuidadorIdoso() { }

    public VinculoCuidadorIdoso(Usuario cuidador, Usuario idoso, Instant criadoEm) {
        this.cuidador = cuidador;
        this.idoso = idoso;
        this.criadoEm = criadoEm;
    }

    public void desfazer(Instant quando) {
        this.ativo = false;
        this.desfeitoEm = quando;
    }

    public Long getId() { return id; }
    public Usuario getCuidador() { return cuidador; }
    public Usuario getIdoso() { return idoso; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getDesfeitoEm() { return desfeitoEm; }
}
