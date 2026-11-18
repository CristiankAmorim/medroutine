CREATE TABLE usuario (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120) NOT NULL,
    email            VARCHAR(160) UNIQUE,
    senha_hash       VARCHAR(100),
    tipo_usuario     VARCHAR(10)  NOT NULL CHECK (tipo_usuario IN ('IDOSO', 'CUIDADOR')),
    fuso_horario     VARCHAR(40)  NOT NULL DEFAULT 'America/Bahia',
    anuncio_voz      BOOLEAN      NOT NULL DEFAULT FALSE,
    fcm_token        VARCHAR(255),
    consentimento_em TIMESTAMPTZ,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CHECK (tipo_usuario = 'IDOSO' OR (email IS NOT NULL AND senha_hash IS NOT NULL))
);

CREATE TABLE vinculo_cuidador_idoso (
    id          BIGSERIAL PRIMARY KEY,
    cuidador_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    idoso_id    BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    ativo       BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    desfeito_em TIMESTAMPTZ
);
CREATE UNIQUE INDEX uk_vinculo_ativo ON vinculo_cuidador_idoso (cuidador_id, idoso_id) WHERE ativo;

CREATE TABLE codigo_acesso (
    id            BIGSERIAL PRIMARY KEY,
    codigo_hash   VARCHAR(64) NOT NULL,
    tipo          VARCHAR(20) NOT NULL CHECK (tipo IN ('ACESSO_IDOSO', 'CONVITE_CUIDADOR', 'RECUPERACAO_SENHA')),
    usuario_id    BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    criado_por_id BIGINT      REFERENCES usuario (id) ON DELETE SET NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_em     TIMESTAMPTZ NOT NULL,
    usado_em      TIMESTAMPTZ
);
CREATE INDEX ix_codigo_hash ON codigo_acesso (codigo_hash, tipo);

CREATE TABLE medicamento (
    id          BIGSERIAL PRIMARY KEY,
    idoso_id    BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nome        VARCHAR(120) NOT NULL,
    dosagem     VARCHAR(60)  NOT NULL,
    orientacoes VARCHAR(500),
    data_inicio DATE         NOT NULL,
    data_fim    DATE,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CHECK (data_fim IS NULL OR data_fim >= data_inicio)
);

CREATE TABLE horario_medicacao (
    id                 BIGSERIAL PRIMARY KEY,
    medicamento_id     BIGINT      NOT NULL REFERENCES medicamento (id) ON DELETE CASCADE,
    hora               TIME        NOT NULL,
    tipo_frequencia    VARCHAR(20) NOT NULL CHECK (tipo_frequencia IN ('DIARIA', 'A_CADA_N_HORAS', 'DIAS_SEMANA')),
    intervalo_horas    INTEGER CHECK (intervalo_horas BETWEEN 1 AND 24),
    dias_semana        VARCHAR(70),
    tolerancia_minutos INTEGER     NOT NULL DEFAULT 30 CHECK (tolerancia_minutos BETWEEN 5 AND 240),
    ativo              BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE dose_programada (
    id                  BIGSERIAL PRIMARY KEY,
    horario_id          BIGINT      NOT NULL REFERENCES horario_medicacao (id) ON DELETE CASCADE,
    data_hora_prevista  TIMESTAMPTZ NOT NULL,
    estado              VARCHAR(25) NOT NULL DEFAULT 'PENDENTE'
        CHECK (estado IN ('PENDENTE', 'CONFIRMADA', 'NAO_CONFIRMADA', 'CONFIRMADA_COM_ATRASO')),
    UNIQUE (horario_id, data_hora_prevista)
);
CREATE INDEX ix_dose_estado_prevista ON dose_programada (estado, data_hora_prevista);

CREATE TABLE confirmacao_dose (
    id               BIGSERIAL PRIMARY KEY,
    dose_id          BIGINT      NOT NULL UNIQUE REFERENCES dose_programada (id) ON DELETE CASCADE,
    usuario_id       BIGINT      REFERENCES usuario (id) ON DELETE SET NULL,
    pelo_cuidador    BOOLEAN     NOT NULL DEFAULT FALSE,
    confirmado_em    TIMESTAMPTZ NOT NULL,
    horario_aparelho TIMESTAMPTZ
);

CREATE TABLE notificacao (
    id              BIGSERIAL PRIMARY KEY,
    dose_id         BIGINT      NOT NULL REFERENCES dose_programada (id) ON DELETE CASCADE,
    destinatario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo            VARCHAR(15) NOT NULL CHECK (tipo IN ('CONFIRMACAO', 'ALERTA')),
    estado_envio    VARCHAR(10) NOT NULL DEFAULT 'PENDENTE' CHECK (estado_envio IN ('PENDENTE', 'ENVIADA', 'FALHA')),
    tentativas      INTEGER     NOT NULL DEFAULT 0,
    ultimo_erro     VARCHAR(255),
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    enviado_em      TIMESTAMPTZ,
    UNIQUE (dose_id, destinatario_id, tipo)
);
