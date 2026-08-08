# ARQUITECTURA — Sistema de Gestión de Proyectos y Tareas

Documento de contexto arquitectónico para la Prueba Técnica Codesa (Desarrollador Fullstack — Spring Boot + Angular).
Este documento es la fuente de verdad de las decisiones de diseño mientras se construye el sistema. Se irá referenciando y ampliando durante el desarrollo, y su versión final pasará a `README.md` / `/docs`.

## 1. Requisitos que gobiernan el diseño

- Plazo: 4 días calendario.
- Stack obligatorio: Java 21 · Spring Boot 3 · Angular 20 · PostgreSQL · Docker Compose.
- Arquitectura de microservicios (más de un servicio backend desplegable, no monolito).
- Autenticación JWT (login/registro, rutas protegidas).
- Frontend Angular desacoplado, consumiendo un único punto de entrada.
- Scheduler `@Scheduled` que marca tareas vencidas como `OVERDUE`.
- Todo debe levantar con un solo comando: `docker compose up --build`, sin pasos manuales.
- Roles `ADMIN` (todo) y `USER` (solo sus propios proyectos/tareas).

## 2. Decisión arquitectónica

### 2.1 Componentes

| Componente | Responsabilidad | Puerto host | Base de datos |
|---|---|---|---|
| `api-gateway` | Único punto de entrada del frontend; enruta a los microservicios | 8080 | — |
| `auth-service` | Registro, login, emisión de JWT, perfil del usuario autenticado | 8081 | `auth_db` |
| `core-service` | CRUD de proyectos y tareas, reglas de negocio, roles, scheduler `OVERDUE` | 8082 | `core_db` |
| `frontend` | SPA Angular 20 | 4200 (dev) / 80 (docker) | — |
| `postgres` | Motor relacional, una instancia con 2 bases de datos | 5432 | `auth_db`, `core_db` |

### 2.2 Por qué 2 microservicios de negocio (no 3)

`Project` y `Task` tienen una relación fuerte (FK directa `task.project_id`) y comparten las mismas reglas de autorización por rol. Separarlos en servicios distintos obligaría a llamadas síncronas constantes solo para validar pertenencia (por ejemplo, "¿esta tarea pertenece a un proyecto mío?"), lo que añade complejidad de comunicación distribuida sin beneficio real dentro de un plazo de 4 días. `auth-service` sí se separa porque es un dominio distinto (identidad), y con eso se cumple el requisito de "más de un servicio backend desplegable".

### 2.3 Punto de entrada único

El frontend **solo** habla con `api-gateway` (Spring Cloud Gateway). El gateway enruta por prefijo de path (`/api/auth/**` → auth-service, `/api/projects/**` y `/api/tasks/**` → core-service). Esto evita que el frontend conozca la topología interna de microservicios y permite mover/escalar servicios sin romper el cliente. Es la causa de rechazo automático más fácil de violar ("frontend que llama microservicios internos saltándose el punto de entrada"), así que el interceptor HTTP de Angular apunta únicamente a la URL base del gateway.

### 2.4 JWT — emisión y validación

- **Emisión:** `auth-service`, al hacer login exitoso, firma un JWT (HS256, secreto compartido vía variable de entorno `JWT_SECRET`) con claims `sub` (userId), `role` (`ADMIN`/`USER`) y `email`.
- **Validación:** cada microservicio de negocio (`core-service`) valida el token por sí mismo (patrón resource-server de Spring Security) usando el mismo secreto. El gateway **no** reimplementa la lógica de autorización, solo enruta — así se evita un único punto de fallo de seguridad y cada servicio sigue siendo responsable de sus propias reglas de acceso.
- Contraseñas: hasheadas con BCrypt, nunca en texto plano (causa de rechazo automático si se incumple).

### 2.5 Relación usuario–proyecto entre servicios/bases distintas

`auth_db` y `core_db` son bases físicamente separadas (sin FK entre ellas). `core-service` no llama a `auth-service` en cada request para resolver el dueño de un proyecto: confía en los claims del JWT (`sub`, `role`) que llegan en cada llamada. La tabla `projects` almacena `owner_id` como un valor plano (el `sub` del JWT), sin integridad referencial física entre bases — la consistencia se garantiza a nivel de aplicación.

### 2.6 Scheduler

Vive en `core-service`, porque es el dueño de la tabla `tasks`. Usa `@Scheduled(cron = "${scheduler.overdue.cron}")`, configurable por propiedad (por defecto cada hora). Busca tareas con `status NOT IN (DONE, OVERDUE)` y `dueDate < hoy`, las actualiza a `OVERDUE` en bloque y registra en log (nivel INFO) cuántas fueron actualizadas.

## 3. Modelo de dominio (resumen)

- **User** (`auth_db`): id, email, passwordHash, role, createdAt.
- **Project** (`core_db`): id, name, description, status (`ACTIVE`/`ARCHIVED`), ownerId, createdAt, updatedAt.
- **Task** (`core_db`): id, projectId (FK a Project), title, description, status (`PENDING`/`IN_PROGRESS`/`DONE`/`OVERDUE`), dueDate, createdAt, updatedAt.

## 4. Reglas de negocio clave

1. `USER` solo ve/opera sus propios proyectos y las tareas dentro de ellos; `ADMIN` opera sobre todo.
2. No se pueden crear tareas en proyectos `ARCHIVED`.
3. `dueDate` no puede ser anterior a la fecha de creación de la tarea.
4. Acceso no autorizado responde HTTP 403.
5. Capas obligatorias: Controller → Service → Repository; DTOs en la API (nunca se exponen entidades JPA).

## 5. Estructura de carpetas del repositorio

```
PruebaCodesa/
├── README.md
├── ARQUITECTURA.md
├── docker-compose.yml
├── docs/
│   ├── modelo-er.*
│   ├── diagrama-componentes.*
│   └── diagrama-secuencia.*
├── api-gateway/          (Spring Cloud Gateway + Dockerfile)
├── auth-service/         (Spring Boot + Dockerfile)
├── core-service/         (Spring Boot + Dockerfile)
└── frontend/             (Angular 20 + Dockerfile)
```

## 6. Orden de construcción

Ver sección "Orden de construcción" impresa en consola al generar este documento; se mantiene sincronizada con el checklist de tareas del proyecto.

## 7. Checklist de causas de rechazo automático (a vigilar en cada etapa)

- [ ] El proyecto debe levantar con `docker compose up --build` sin pasos manuales.
- [ ] Ninguna contraseña en texto plano (BCrypt en auth-service).
- [ ] El frontend nunca llama directamente a `auth-service` o `core-service`, solo al `api-gateway`.
- [ ] Repositorio público al finalizar.
- [ ] Más de un servicio backend real (no todo el dominio en uno solo).

## 8. Estado del entorno de desarrollo

Verificado el 2026-08-08: Java 21 (Temurin), Maven 3.9.16, Docker 29.6.2 + Compose v5.3.1, Node 22.23.2 (default vía nvm), Angular CLI 20, Git 2.50.1. Para compilar el backend localmente fuera de Docker, fijar `JAVA_HOME` a la 21: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
