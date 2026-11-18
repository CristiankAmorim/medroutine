package br.ucsal.medroutine.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medicamento")
public class Medicamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idoso_id")
    private Usuario idoso;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 60)
    private String dosagem;

    @Column(length = 500)
    private String orientacoes;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "medicamento", cascade = CascadeType.ALL)
    @OrderBy("hora ASC")
    private List<HorarioMedicacao> horarios = new ArrayList<>();

    protected Medicamento() { }

    public Medicamento(Usuario idoso) {
        this.idoso = idoso;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Usuario getIdoso() { return idoso; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDosagem() { return dosagem; }
    public void setDosagem(String dosagem) { this.dosagem = dosagem; }
    public String getOrientacoes() { return orientacoes; }
    public void setOrientacoes(String orientacoes) { this.orientacoes = orientacoes; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public List<HorarioMedicacao> getHorarios() { return horarios; }
}
