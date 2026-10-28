-- FarmaGuard - estructura de la base de datos SQLite
-- Importante: Spring separa las sentencias por punto y coma, no usar ";" dentro de textos.

CREATE TABLE IF NOT EXISTS medicamentos (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre                TEXT    NOT NULL UNIQUE,
    -- Uno o varios principios activos separados por "+" (ej. "Paracetamol + Clorfenamina")
    principio_activo      TEXT    NOT NULL,
    categoria             TEXT    NOT NULL,
    concentracion         TEXT,
    -- mg del primer principio activo por unidad (tableta, cápsula, sobre)
    dosis_mg              REAL,
    -- máximo recomendado por unidad sin receta (si dosis_mg lo supera se avisa "concentración alta")
    dosis_max_unidad_mg   REAL,
    -- máximo diario del primer principio activo para un adulto
    dosis_max_diaria_mg   REAL,
    unidades_dia_habitual INTEGER NOT NULL DEFAULT 1,
    dosis_sugerida        TEXT,
    momento_comida        TEXT    NOT NULL DEFAULT 'INDIFERENTE'
                          CHECK (momento_comida IN ('ANTES', 'DURANTE', 'DESPUES', 'INDIFERENTE')),
    requiere_receta       INTEGER NOT NULL DEFAULT 0,
    descripcion           TEXT    NOT NULL,
    consejos              TEXT
);

CREATE INDEX IF NOT EXISTS idx_medicamentos_principio ON medicamentos (principio_activo);

-- Las interacciones se definen entre principios activos (no entre marcas),
-- así una regla "Ibuprofeno + Naproxeno" cubre también Advil + Apronax.
CREATE TABLE IF NOT EXISTS interacciones (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    principio_a  TEXT NOT NULL,
    principio_b  TEXT NOT NULL,
    nivel_riesgo TEXT NOT NULL CHECK (nivel_riesgo IN ('PRECAUCION', 'PELIGRO')),
    explicacion  TEXT NOT NULL,
    UNIQUE (principio_a, principio_b)
);

CREATE TABLE IF NOT EXISTS recordatorios (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    medicamento      TEXT    NOT NULL,
    frecuencia_horas INTEGER NOT NULL CHECK (frecuencia_horas BETWEEN 1 AND 24),
    duracion_dias    INTEGER NOT NULL CHECK (duracion_dias BETWEEN 1 AND 365),
    -- cada cuántos días se repite: 1 = diario, 7 = una vez por semana (ej. metotrexato)
    intervalo_dias   INTEGER NOT NULL DEFAULT 1 CHECK (intervalo_dias BETWEEN 1 AND 30),
    momento_comida   TEXT    NOT NULL DEFAULT 'INDIFERENTE'
                     CHECK (momento_comida IN ('ANTES', 'DURANTE', 'DESPUES', 'INDIFERENTE')),
    hora_inicio      TEXT    NOT NULL, -- HH:mm
    fecha_inicio     TEXT    NOT NULL, -- yyyy-MM-dd
    notas            TEXT,
    activo           INTEGER NOT NULL DEFAULT 1
);
