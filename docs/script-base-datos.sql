-- =====================================================================
-- MicrosConsultas — Script de creacion de base de datos + datos de prueba
-- =====================================================================
-- Reconstruye desde cero el esquema de las dos bases del stack
-- (auth_db, core_db — ver ARQUITECTURA.md seccion 2.5, database-per-service)
-- y carga la misma data de prueba que existia en el ambiente de desarrollo
-- al momento de generar este script.
--
-- El esquema es identico al de las migraciones Flyway del proyecto:
--   auth-service/src/main/resources/db/migration/V1__create_users.sql
--   core-service/src/main/resources/db/migration/V1__create_projects_and_tasks.sql
--
-- Uso:
--   psql -U postgres -h localhost -f script-base-datos.sql
--
-- Requisitos:
--   - Se asume una instancia de PostgreSQL nueva (sin las bases auth_db/core_db
--     creadas todavia). Si ya existen, elimínalas antes o comenta las lineas
--     CREATE DATABASE.
--
-- IMPORTANTE — NO ejecutar contra el Postgres del docker-compose del proyecto
-- (contenedor "pruebacodesa-postgres"): auth-service y core-service versionan
-- su esquema con Flyway mediante una tabla flyway_schema_history. Si las
-- tablas se crean por fuera de Flyway (como hace este script), el servicio
-- fallara al arrancar la proxima vez porque Flyway intentara re-aplicar la
-- migracion V1 sobre tablas que ya existen. Este script esta pensado para
-- una instancia de Postgres aparte (ver README.md, seccion "Inspeccionar el
-- esquema y datos de prueba fuera del stack").
--
-- NOTA: los password_hash de la seccion de usuarios son hashes BCrypt reales
-- de credenciales de PRUEBA usadas durante el desarrollo (no son secretos de
-- produccion). Contraseñas en texto plano de referencia, solo para este
-- ambiente de demo:
--   admin@codesa.com          -> Admin1234   (rol ADMIN)
--   walter@codesa.com         -> Walter1234  (rol USER)
--   otro@codesa.com           -> Otro12345   (rol USER)
--   steven.alvear@codesa.com  -> (definida por el usuario en la UI)
--   e2e-check@codesa.com      -> E2eCheck123 (rol USER)
--   walter@test.com           -> (usuario de prueba de una sesion anterior)
-- =====================================================================


-- =====================================================================
-- 1. Creacion de las bases de datos
-- =====================================================================
CREATE DATABASE auth_db;
CREATE DATABASE core_db;


-- =====================================================================
-- 2. auth_db — esquema y datos
-- =====================================================================
\c auth_db

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);

INSERT INTO users (id, email, password_hash, role, created_at) VALUES
    (1, 'walter@test.com',          '$2a$10$PgHAjE6tMoCdG/.8PMcyBuR1jb8mu7cipkqIuOgpJaLeulUUkkGWS', 'USER',  '2026-08-08 19:48:31.011673+00'),
    (2, 'admin@codesa.com',         '$2a$10$OFXhsOyx2xCv5/JETNS6LOp3wLlq.4aQ6Nsmoe91Ue.JvRQdSmMoW', 'ADMIN', '2026-08-08 20:04:31.300958+00'),
    (3, 'walter@codesa.com',        '$2a$10$dxH9V4VAPMtiScIgeQEkZ.Qg.K/PWVnnPjq0jovdtN8styrMIyfjq', 'USER',  '2026-08-08 20:04:31.446508+00'),
    (4, 'otro@codesa.com',          '$2a$10$UgJEwEpzeoivPaD4uQEgTOoVnzGagnUOAM9TyJX2zgZgVBRTafbQi', 'USER',  '2026-08-08 20:04:31.535297+00'),
    (5, 'steven.alvear@codesa.com', '$2a$10$lbOWtHHNBcMoQWFkrPTIHu/1vNXKfcJ1.Rr/q1GrDLBAAOx8jpP9u', 'USER',  '2026-08-08 20:21:49.038861+00'),
    (6, 'e2e-check@codesa.com',     '$2a$10$kMywA9NybPs.QLTlGpLx8e2cy.roPvc43KJqFEG.imekh7GOwG/Vm', 'USER',  '2026-08-08 21:47:21.531839+00');

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));


-- =====================================================================
-- 3. core_db — esquema y datos
-- =====================================================================
\c core_db

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL,
    owner_id    BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_owner_id ON projects (owner_id);

CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL,
    due_date    DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_tasks_status_due_date ON tasks (status, due_date);

-- owner_id referencia logicamente a auth_db.users.id (claim "sub" del JWT);
-- sin FK fisica entre bases (ver ARQUITECTURA.md seccion 2.5).
INSERT INTO projects (id, name, description, status, owner_id, created_at, updated_at) VALUES
    (2, 'Portal Clientes',     'Portal de autoservicio para clientes',      'ACTIVE',   3, '2026-08-08 20:05:04.741455+00', '2026-08-08 20:15:04.919359+00'),
    (3, 'Migracion Legacy',    'Migracion del sistema legado',              'ARCHIVED', 3, '2026-08-08 20:05:04.807460+00', '2026-08-08 20:05:33.710293+00'),
    (5, 'Proyecto Steven',     'Este proyecto solo debe verlo Steven',      'ACTIVE',   5, '2026-08-08 20:22:07.258444+00', '2026-08-08 20:22:07.258444+00'),
    (6, 'Diseño UX',           'Mejora de diseño gráfico y experiencia',    'ACTIVE',   2, '2026-08-08 20:40:22.837829+00', '2026-08-08 20:40:22.837829+00'),
    (7, 'Verificacion Lombok', 'Prueba post-refactor',                      'ACTIVE',   3, '2026-08-08 21:46:13.741791+00', '2026-08-08 21:46:13.741791+00'),
    (8, 'E2E Check Project',   'Validacion end-to-end',                     'ACTIVE',   6, '2026-08-08 21:47:21.871048+00', '2026-08-08 21:47:21.871048+00');

INSERT INTO tasks (id, project_id, title, description, status, due_date, created_at, updated_at) VALUES
    (2, 2, 'Disenar login',         'Wireframes de login y registro',                       'DONE',        '2026-08-20', '2026-08-08 20:05:33.476770+00', '2026-08-08 20:05:33.675866+00'),
    (3, 2, 'Implementar dashboard', NULL,                                                    'PENDING',     '2026-08-25', '2026-08-08 20:05:33.506352+00', '2026-08-08 20:17:28.500189+00'),
    (4, 2, 'QA de checkout',        NULL,                                                    'PENDING',     '2026-08-10', '2026-08-08 20:05:33.531754+00', '2026-08-08 20:05:33.531754+00'),
    (5, 5, 'Primera prueba',        'Se debe ajustar el front. Para que se vea más bonito', 'IN_PROGRESS', '2026-08-08', '2026-08-08 20:22:41.268986+00', '2026-08-08 20:22:44.900678+00'),
    (6, 7, 'Tarea post-Lombok',     NULL,                                                    'PENDING',     '2026-09-01', '2026-08-08 21:46:13.888700+00', '2026-08-08 21:46:13.888700+00'),
    (7, 8, 'E2E check task',        NULL,                                                    'PENDING',     '2026-09-15', '2026-08-08 21:47:21.962802+00', '2026-08-08 21:47:21.962802+00');

SELECT setval(pg_get_serial_sequence('projects', 'id'), (SELECT MAX(id) FROM projects));
SELECT setval(pg_get_serial_sequence('tasks', 'id'), (SELECT MAX(id) FROM tasks));
