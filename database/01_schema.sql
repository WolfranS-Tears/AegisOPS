-- =====================================================================
-- AegisOPS - Esquema relacional (PostgreSQL 17)
-- Se ejecuta automáticamente al crear el contenedor de PostgreSQL
-- (docker-entrypoint-initdb.d). Hibernate solo valida (ddl-auto: validate).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuario: responsable de atender incidentes
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id      BIGSERIAL    PRIMARY KEY,
    nombre  VARCHAR(120) NOT NULL,
    correo  VARCHAR(150) NOT NULL,
    area    VARCHAR(80)  NOT NULL,
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_correo CHECK (correo LIKE '%_@_%')
);

-- ---------------------------------------------------------------------
-- Categoria de incidentes y artículos
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id          BIGSERIAL    PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL,
    descripcion VARCHAR(300),
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- Incidente
-- Regla: un incidente RESUELTO o CERRADO tiene fecha_resolucion y es
-- posterior (o igual) a su fecha de creación; uno abierto no la tiene.
-- ---------------------------------------------------------------------
CREATE TABLE incidente (
    id                  BIGSERIAL    PRIMARY KEY,
    titulo              VARCHAR(200) NOT NULL,
    descripcion         TEXT         NOT NULL,
    categoria_id        BIGINT       NOT NULL,
    prioridad           VARCHAR(10)  NOT NULL,
    estado              VARCHAR(15)  NOT NULL DEFAULT 'ABIERTO',
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion    TIMESTAMP,
    usuario_id          BIGINT       NOT NULL,
    CONSTRAINT fk_incidente_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_incidente_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuario (id),
    CONSTRAINT ck_incidente_prioridad CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_incidente_estado    CHECK (estado IN ('ABIERTO', 'EN_PROGRESO', 'RESUELTO', 'CERRADO')),
    CONSTRAINT ck_incidente_titulo    CHECK (length(trim(titulo)) > 0),
    CONSTRAINT ck_incidente_desc      CHECK (length(trim(descripcion)) > 0),
    CONSTRAINT ck_incidente_resolucion CHECK (
        (estado IN ('RESUELTO', 'CERRADO') AND fecha_resolucion IS NOT NULL AND fecha_resolucion >= fecha_creacion)
        OR (estado IN ('ABIERTO', 'EN_PROGRESO') AND fecha_resolucion IS NULL)
    )
);

CREATE INDEX idx_incidente_categoria      ON incidente (categoria_id);
CREATE INDEX idx_incidente_usuario        ON incidente (usuario_id);
CREATE INDEX idx_incidente_estado         ON incidente (estado);
CREATE INDEX idx_incidente_prioridad      ON incidente (prioridad);
CREATE INDEX idx_incidente_fecha_creacion ON incidente (fecha_creacion);

-- ---------------------------------------------------------------------
-- ArticuloConocimiento: base de conocimiento técnica.
-- Solo los artículos ACTIVO + verificado = true pueden usarse como fuente.
-- ---------------------------------------------------------------------
CREATE TABLE articulo_conocimiento (
    id                  BIGSERIAL    PRIMARY KEY,
    titulo              VARCHAR(200) NOT NULL,
    problema            TEXT         NOT NULL,
    sintomas            TEXT         NOT NULL,
    causa               TEXT         NOT NULL,
    solucion            TEXT         NOT NULL,
    categoria_id        BIGINT       NOT NULL,
    estado              VARCHAR(10)  NOT NULL DEFAULT 'BORRADOR',
    verificado          BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_articulo_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_articulo_estado    CHECK (estado IN ('BORRADOR', 'ACTIVO', 'INACTIVO')),
    CONSTRAINT uk_articulo_titulo    UNIQUE (titulo)
);

CREATE INDEX idx_articulo_categoria ON articulo_conocimiento (categoria_id);
CREATE INDEX idx_articulo_fuente    ON articulo_conocimiento (estado, verificado);

-- ---------------------------------------------------------------------
-- ConsultaIA: cada consulta al asistente RAG
-- ---------------------------------------------------------------------
CREATE TABLE consulta_ia (
    id            BIGSERIAL     PRIMARY KEY,
    incidente_id  BIGINT,
    pregunta      TEXT          NOT NULL,
    fecha         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado        VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE',
    umbral        NUMERIC(4, 3) NOT NULL,
    mensaje_error VARCHAR(500),
    CONSTRAINT fk_consulta_incidente FOREIGN KEY (incidente_id) REFERENCES incidente (id) ON DELETE SET NULL,
    CONSTRAINT ck_consulta_estado    CHECK (estado IN ('PENDIENTE', 'RESPONDIDA', 'SIN_RESULTADOS', 'ERROR')),
    CONSTRAINT ck_consulta_pregunta  CHECK (length(trim(pregunta)) > 0),
    CONSTRAINT ck_consulta_umbral    CHECK (umbral >= 0 AND umbral <= 1)
);

CREATE INDEX idx_consulta_incidente ON consulta_ia (incidente_id);
CREATE INDEX idx_consulta_fecha     ON consulta_ia (fecha);
CREATE INDEX idx_consulta_estado    ON consulta_ia (estado);

-- ---------------------------------------------------------------------
-- RespuestaIA: respuesta generada (1 por consulta)
-- ---------------------------------------------------------------------
CREATE TABLE respuesta_ia (
    id          BIGSERIAL    PRIMARY KEY,
    consulta_id BIGINT       NOT NULL,
    respuesta   TEXT         NOT NULL,
    pasos       TEXT,
    modelo      VARCHAR(100),
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_respuesta_consulta FOREIGN KEY (consulta_id) REFERENCES consulta_ia (id) ON DELETE CASCADE,
    CONSTRAINT uk_respuesta_consulta UNIQUE (consulta_id)
);

-- ---------------------------------------------------------------------
-- FuenteRespuesta: artículos usados como fuente y su similitud
-- ---------------------------------------------------------------------
CREATE TABLE fuente_respuesta (
    id           BIGSERIAL     PRIMARY KEY,
    respuesta_id BIGINT        NOT NULL,
    articulo_id  BIGINT        NOT NULL,
    similitud    NUMERIC(5, 4) NOT NULL,
    CONSTRAINT fk_fuente_respuesta FOREIGN KEY (respuesta_id) REFERENCES respuesta_ia (id) ON DELETE CASCADE,
    CONSTRAINT fk_fuente_articulo  FOREIGN KEY (articulo_id)  REFERENCES articulo_conocimiento (id),
    CONSTRAINT uk_fuente           UNIQUE (respuesta_id, articulo_id),
    CONSTRAINT ck_fuente_similitud CHECK (similitud >= 0 AND similitud <= 1)
);

CREATE INDEX idx_fuente_articulo ON fuente_respuesta (articulo_id);

-- ---------------------------------------------------------------------
-- Calificacion: utilidad de la respuesta (1 a 5). Una por respuesta.
-- ---------------------------------------------------------------------
CREATE TABLE calificacion (
    id           BIGSERIAL    PRIMARY KEY,
    respuesta_id BIGINT       NOT NULL,
    puntuacion   SMALLINT     NOT NULL,
    comentario   VARCHAR(500),
    fecha        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calificacion_respuesta FOREIGN KEY (respuesta_id) REFERENCES respuesta_ia (id) ON DELETE CASCADE,
    CONSTRAINT uk_calificacion_respuesta UNIQUE (respuesta_id),
    CONSTRAINT ck_calificacion_puntuacion CHECK (puntuacion BETWEEN 1 AND 5)
);
