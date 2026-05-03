CREATE TABLE pauta (
    id          BIGINT        GENERATED ALWAYS AS IDENTITY,
    titulo      VARCHAR(200)  NOT NULL,
    descricao   VARCHAR(2000),
    criada_em   TIMESTAMP     NOT NULL,
    CONSTRAINT pk_pauta PRIMARY KEY (id)
);

CREATE TABLE sessao (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY,
    pauta_id    BIGINT      NOT NULL,
    aberta_em   TIMESTAMP   NOT NULL,
    fecha_em    TIMESTAMP   NOT NULL,
    CONSTRAINT pk_sessao PRIMARY KEY (id),
    CONSTRAINT uk_sessao_pauta UNIQUE (pauta_id),
    CONSTRAINT fk_sessao_pauta FOREIGN KEY (pauta_id) REFERENCES pauta(id),
    CONSTRAINT ck_sessao_intervalo CHECK (fecha_em > aberta_em)
);

CREATE TABLE voto (
    id              BIGINT      GENERATED ALWAYS AS IDENTITY,
    pauta_id        BIGINT      NOT NULL,
    associado_id    VARCHAR(64) NOT NULL,
    escolha         VARCHAR(3)  NOT NULL,
    registrado_em   TIMESTAMP   NOT NULL,
    CONSTRAINT pk_voto PRIMARY KEY (id),
    CONSTRAINT uk_voto_pauta_associado UNIQUE (pauta_id, associado_id),
    CONSTRAINT fk_voto_pauta FOREIGN KEY (pauta_id) REFERENCES pauta(id),
    CONSTRAINT ck_voto_escolha CHECK (escolha IN ('SIM', 'NAO'))
);

CREATE INDEX idx_voto_pauta ON voto (pauta_id);
