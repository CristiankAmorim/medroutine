package br.ucsal.medroutine.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 160, unique = true)
    private String email;

    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 10)
    private TipoUsuario tipoUsuario;

    @Column(name = "fuso_horario", nullable = false, length = 40)
    private String fusoHorario = "America/Bahia";

    @Column(name = "anuncio_voz", nullable = false)
    private boolean anuncioVoz;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "consentimento_em")
    private Instant consentimentoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public ZoneId zona() {
        return ZoneId.of(fusoHorario);
    }

    public boolean isIdoso() { return tipoUsuario == TipoUsuario.IDOSO; }
    public boolean isCuidador() { return tipoUsuario == TipoUsuario.CUIDADOR; }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public TipoUsuario getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }
    public String getFusoHorario() { return fusoHorario; }
    public void setFusoHorario(String fusoHorario) { this.fusoHorario = fusoHorario; }
    public boolean isAnuncioVoz() { return anuncioVoz; }
    public void setAnuncioVoz(boolean anuncioVoz) { this.anuncioVoz = anuncioVoz; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public Instant getConsentimentoEm() { return consentimentoEm; }
    public void setConsentimentoEm(Instant consentimentoEm) { this.consentimentoEm = consentimentoEm; }
    public Instant getCriadoEm() { return criadoEm; }
}
