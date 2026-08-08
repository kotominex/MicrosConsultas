# MicrosConsultas — Sistema de Gestión de Proyectos y Tareas

Prueba técnica Codesa (Desarrollador Fullstack — Spring Boot + Angular). Arquitectura de microservicios con autenticación JWT, roles `ADMIN`/`USER` y un scheduler que marca tareas vencidas automáticamente.

La documentación completa de las decisiones de diseño está en [`ARQUITECTURA.md`](./ARQUITECTURA.md).

## Stack

Java 21 · Spring Boot 3 · Spring Cloud Gateway · Angular 20 · PostgreSQL 16 · Docker Compose.

## Componentes

| Componente | Responsabilidad | Puerto host |
|---|---|---|
| `frontend` | SPA Angular 20 (Nginx en producción) | 4200 |
| `api-gateway` | Único punto de entrada; enruta al resto de servicios | 8080 |
| `auth-service` | Registro, login, emisión de JWT, perfil de usuario | 8081 |
| `core-service` | CRUD de proyectos/tareas, reglas de negocio, scheduler `OVERDUE` | 8082 |
| `postgres` | Motor relacional, una instancia con 2 bases (`auth_db`, `core_db`) | 5432 |

El frontend solo se comunica con `api-gateway`; ningún servicio de negocio es accesible directamente desde el navegador salvo por sus puertos expuestos para pruebas/depuración.

## Requisitos previos

- Docker y Docker Compose (`docker compose version`).
- No se necesita Java, Node ni Maven instalados localmente: todo se compila dentro de los contenedores.

## Puesta en marcha

```bash
docker compose up --build
```

Con eso ya queda arriba todo el stack, sin ningún paso manual previo: construye las 4 imágenes, crea `auth_db`/`core_db` en el primer arranque de Postgres (`postgres/init/01-init-databases.sh`), cada microservicio aplica sus propias migraciones Flyway al iniciar, y todas las variables de entorno (incluida `JWT_SECRET`) tienen un valor por defecto en `docker-compose.yml`. El stack queda funcional (con las tablas **vacías**) con solo clonar el repo y ejecutar ese comando.

Abrir el frontend en [http://localhost:4200](http://localhost:4200).

### (Opcional) Personalizar variables de entorno

Para usar tus propios valores (por ejemplo, un `JWT_SECRET` propio si esto se va a exponer más allá de un entorno local/evaluación, u otros puertos), copiá el archivo de ejemplo antes de levantar el stack:

```bash
cp .env.example .env
```

`docker-compose.yml` lo lee automáticamente si existe; si no existe, usa los valores por defecto documentados en `.env.example`.

Para detener y limpiar:

```bash
docker compose down
```

### (Opcional) Inspeccionar el esquema y datos de prueba fuera del stack

`docs/script-base-datos.sql` es un artefacto de documentación, **independiente de `docker compose`** — no se ejecuta automáticamente ni está pensado para cargarse dentro del Postgres que gestiona el stack. Recrea desde cero el esquema completo (idéntico a las migraciones Flyway) más usuarios/proyectos/tareas de ejemplo, incluyendo un `ADMIN` (credenciales documentadas en el encabezado del script).

⚠️ No lo ejecutes contra `pruebacodesa-postgres` (el contenedor del stack): cada microservicio usa Flyway para versionar su esquema mediante una tabla `flyway_schema_history`; si las tablas se crean por fuera de Flyway (como hace este script), el servicio fallará al arrancar la próxima vez porque Flyway intentará re-aplicar la migración `V1` sobre tablas que ya existen.

Está pensado para levantarlo en una instancia de Postgres aparte (por ejemplo, para revisar el modelo o los datos con un cliente SQL sin necesitar el resto del stack):

```bash
docker run -d --name revision-bd -e POSTGRES_PASSWORD=postgres -p 5555:5432 postgres:16-alpine
docker cp docs/script-base-datos.sql revision-bd:/tmp/script-base-datos.sql
docker exec -it revision-bd psql -U postgres -f /tmp/script-base-datos.sql

# conectarse para explorar:
docker exec -it revision-bd psql -U postgres -d core_db

# al terminar:
docker rm -f revision-bd
```

## Autenticación y roles

- **Registro/login** (`POST /api/auth/register`, `POST /api/auth/login`) devuelven un JWT firmado (HS256) con claims `sub` (userId), `email` y `role`.
- Todo endpoint de negocio requiere `Authorization: Bearer <token>`.
- **`USER`**: solo ve y opera sus propios proyectos y las tareas dentro de ellos.
- **`ADMIN`**: opera sobre todos los proyectos y tareas.
- Acceso no autorizado responde `403 Forbidden`.

## API (vía `api-gateway`, `http://localhost:8080`)

### Auth (`/api/auth`)

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Crea un usuario (rol `USER`) y devuelve un token | No |
| POST | `/api/auth/login` | Autentica y devuelve un token | No |
| GET | `/api/auth/me` | Perfil del usuario autenticado | Sí |

### Proyectos (`/api/projects`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/projects` | Crea un proyecto (el creador queda como `ownerId`) |
| GET | `/api/projects` | Lista proyectos (propios o todos si es `ADMIN`) |
| GET | `/api/projects/{id}` | Detalle de un proyecto |
| PUT | `/api/projects/{id}` | Actualiza nombre/descripción/estado (`ACTIVE`/`ARCHIVED`) |
| DELETE | `/api/projects/{id}` | Elimina un proyecto |

### Tareas (`/api/tasks`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/tasks` | Crea una tarea (rechaza proyectos `ARCHIVED` o `dueDate` anterior a hoy) |
| GET | `/api/tasks?projectId={id}` | Lista tareas de un proyecto |
| GET | `/api/tasks` | Lista todas las tareas accesibles por el usuario |
| GET | `/api/tasks/{id}` | Detalle de una tarea |
| PUT | `/api/tasks/{id}` | Actualiza título/descripción/estado/fecha |
| DELETE | `/api/tasks/{id}` | Elimina una tarea |

Todas las respuestas de error siguen el mismo formato:

```json
{
  "timestamp": "2026-08-08T19:48:31.731Z",
  "status": 403,
  "error": "Forbidden",
  "message": "No tiene permisos para acceder a este recurso",
  "details": []
}
```

## Reglas de negocio

1. `USER` solo ve/opera sus propios proyectos y tareas; `ADMIN` opera sobre todo.
2. No se pueden crear tareas en proyectos `ARCHIVED`.
3. `dueDate` no puede ser anterior a la fecha de creación de la tarea.
4. Un scheduler en `core-service` (`@Scheduled`, cron configurable vía `SCHEDULER_OVERDUE_CRON`, cada hora por defecto) marca como `OVERDUE` las tareas con `status` distinto de `DONE`/`OVERDUE` cuya `dueDate` ya pasó.

## Documentación en Swagger

Con cada backend levantado, la documentación OpenAPI está disponible en:

- `auth-service`: http://localhost:8081/swagger-ui.html
- `core-service`: http://localhost:8082/swagger-ui.html

## Ejecutar tests

```bash
# auth-service
cd auth-service && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test

# core-service
cd core-service && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test

# frontend
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless
```

## Estructura del repositorio

```
PruebaCodesa/
├── README.md
├── ARQUITECTURA.md
├── docker-compose.yml
├── .env.example
├── docs/
│   ├── modelo-er.pdf              (diagrama entidad-relacion + detalle de tablas)
│   ├── diagrama-componentes.pdf   (vista de contenedores + capas internas)
│   ├── diagrama-secuencia.pdf     (login, crear tarea, scheduler OVERDUE)
│   └── script-base-datos.sql     (esquema + datos de prueba, ver seccion anterior)
├── postgres/init/          (script de creacion de auth_db / core_db)
├── api-gateway/            (Spring Cloud Gateway + Dockerfile)
├── auth-service/           (Spring Boot + Dockerfile)
├── core-service/           (Spring Boot + Dockerfile)
└── frontend/               (Angular 20 + Dockerfile)
```
