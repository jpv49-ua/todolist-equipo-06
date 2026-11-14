# ToDoList

Aplicación ToDoList de la asignatura [MADS](https://cvnet.cpd.ua.es/Guia-Docente/GuiaDocente/Index?wlengua=es&wcodasi=34037&scaca=2026-27) desarrollada con Spring Boot y plantillas Thymeleaf.

Versión actual: **1.2.0**

## Funcionalidades

- Registro, login y logout de usuarios.
- Gestión de tareas propias: listar, crear, editar y borrar.
- Barra de menú común con enlaces a la página "acerca de", a las tareas y al desplegable del usuario.
- Usuario administrador: se puede marcar al registrarse, pero solo puede existir uno.
- Listado de usuarios registrados (`/registrados`) y descripción de cada usuario (`/registrados/{id}`), accesibles solo para el administrador.
- Bloqueo y habilitación de usuarios por parte del administrador. Un usuario bloqueado no puede hacer login.
- Tras registrarse, el usuario entra directamente en la aplicación sin tener que hacer login.
- Página de cuenta (`/cuenta`) con los datos del usuario logeado, accesible desde el desplegable del usuario.
- Comentario en las tareas: cada tarea puede tener un comentario, que se muestra junto a ella en el listado de tareas. En el listado, la columna *Tarea* tiene los botones para editar y borrar la tarea, y la columna *Comentario* los botones para añadir, editar y borrar el comentario. El comentario se escribe en la página `/tareas/{id}`.
- Equipos: cualquier usuario puede crear equipos, ver sus miembros y unirse o salir de ellos. El administrador puede además cambiar el nombre de un equipo y eliminarlo.

## Requisitos

- Java 17 o superior (el proyecto compila para Java 17).
- No hace falta instalar Maven: el proyecto incluye el *wrapper* `./mvnw`.
- Opcional: Docker, para ejecutar la aplicación en un contenedor.

## Instalación

```
$ git clone https://github.com/jpv49-ua/todolist-equipo-06.git
$ cd todolist-equipo-06
```

## Puesta en marcha

### Con Maven

```
$ ./mvnw spring-boot:run
```

### Generando el jar

```
$ ./mvnw clean package
$ java -jar target/todolist-equipo-06-1.3.0-SNAPSHOT.jar
```

### Con Docker

```
$ ./mvnw clean package
$ docker build -t mads-todolist .
$ docker run -p 8080:8080 mads-todolist
```

En los tres casos la aplicación queda disponible en [http://localhost:8080/login](http://localhost:8080/login).

### Con PostgreSQL

Por defecto se usa H2. Para usar PostgreSQL hay dos perfiles:

- `postgres` (`application-postgres.properties`): usa `ddl-auto=update`, así que crea las tablas solo.
- `postgres-prod` (`application-postgres-prod.properties`): usa `ddl-auto=validate`, así que el esquema tiene que existir antes. Hay que cargar `sql/schema-1.2.0.sql` o `sql/backup11112026.sql`, y después la migración `sql/schema-1.2.0-1.3.0.sql`, que añade la columna `comentario` a la tabla `tareas`.

Los dos perfiles se conectan a la base de datos `mads` usando las variables `POSTGRES_HOST`, `POSTGRES_PORT`, `DB_USER` y `DB_PASSWD` (por defecto `localhost:5432`, `mads`/`mads`). Se pueden sobrescribir con variables de entorno o con argumentos (`--POSTGRES_HOST=...`). El `ENTRYPOINT` del `Dockerfile` pasa los argumentos a `java -jar`, así que `docker run ... imagen --spring.profiles.active=...` funciona.

Al activar uno de estos perfiles se sustituye el perfil `dev`, así que `InitDbService` no crea el usuario de ejemplo.

#### A) App en local y base de datos en un contenedor

```
$ docker run -d --name postgres-develop -p 5432:5432 -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads postgres:13

# Perfil postgres (crea las tablas solo)
$ ./mvnw spring-boot:run -D spring-boot.run.profiles=postgres

# O perfil postgres-prod (validate): antes hay que cargar el esquema o el backup
$ docker exec -i postgres-develop psql -U mads mads < sql/schema-1.2.0.sql
$ docker exec -i postgres-develop psql -U mads mads < sql/schema-1.2.0-1.3.0.sql
$ ./mvnw spring-boot:run -D spring-boot.run.profiles=postgres-prod
```

#### B) App y base de datos en contenedores en la misma red (como en producción)

```
$ docker network create network-equipo
$ docker run -d --name postgres-prod --network network-equipo -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads postgres:13
$ docker exec -i postgres-prod psql -U mads mads < sql/backup11112026.sql
$ docker exec -i postgres-prod psql -U mads mads < sql/schema-1.2.0-1.3.0.sql

$ ./mvnw clean package
$ docker build -t mads-todolist .
$ docker run --rm --network network-equipo -p 8080:8080 mads-todolist --spring.profiles.active=postgres-prod --POSTGRES_HOST=postgres-prod
```

Dentro de la red, la app llega a la base de datos por el nombre del contenedor (`postgres-prod`), no por `localhost`. No hace falta publicar el puerto 5432 porque solo la app accede a la base de datos.

El backup es un volcado hecho con `--clean`, así que sus primeras órdenes son `DROP`. En una base de datos vacía esas órdenes dan error, pero `psql` sigue con el resto y el volcado se carga bien.

#### Comprobación

- Abre [http://localhost:8080/login](http://localhost:8080/login) y entra con un usuario del backup, o regístrate.
- `docker exec -it postgres-prod psql -U mads mads -c '\dt'` debería listar las tablas `usuarios`, `tareas`, `equipos` y `equipo_usuario`.
- Si con `postgres-prod` aparece el error `Schema-validation: missing table`, es que no se ha cargado el esquema.
- Para limpiar: `docker rm -f postgres-prod postgres-develop && docker network rm network-equipo`.

### Datos iniciales

La aplicación arranca con el perfil `dev` y usa una base de datos H2 en memoria, así que los datos se pierden al pararla.

Al arrancar, la clase `InitDbService` (`src/main/java/madstodolist/service/InitDbService.java`) crea un usuario de ejemplo con las tareas "Lavar coche" y "Renovar DNI":

| Email | Contraseña | Nombre |
|---|---|---|
| `user@ua` | `123` | Usuario Ejemplo |

No existe ningún administrador al arrancar. Para crearlo, regístrate en `/registro` marcando la casilla *Registrar como administrador*. La casilla solo aparece mientras no haya ningún administrador.

La consola de H2 está disponible en [http://localhost:8080/h2-console](http://localhost:8080/h2-console) con la URL JDBC `jdbc:h2:mem:dev`.

## Tests

```
$ ./mvnw test
```

Para ejecutarlos contra PostgreSQL, que es lo que hace la integración continua:

```
$ ./mvnw test -D spring.profiles.active=postgres
```

El perfil `postgres` de los tests (`src/test/resources/application-postgres.properties`) usa la base de datos `mads_test`, no `mads`, así que hay que crearla antes. Por ejemplo, con el contenedor del modo A:

```
$ docker exec postgres-develop createdb -U mads mads_test
```

Por defecto, los tests usan su propia base de datos H2 en memoria (`src/test/resources/application.properties`) y la limpian antes de cada test con `clean-db.sql`.

- **Repositorio** (`repository`): mapeo JPA de las entidades.
- **Servicio** (`service`): lógica de negocio contra la base de datos.
- **Web** (`controller`): peticiones HTTP con `MockMvc`. El `ManagerUserSession` se moquea para simular el usuario logeado.

### Integración continua

Hay dos workflows de GitHub Actions en `.github/workflows`, que se ejecutan en cada `push` con JDK 17 (Temurin):

- **Developer tests**: lanza `./mvnw test` con la base de datos H2.
- **Integration tests**: levanta un contenedor PostgreSQL 13 y lanza los tests con el perfil `postgres`.

## Funcionamiento interno

La aplicación sigue una arquitectura en capas. Todo el código está en el paquete `madstodolist`:

```
Navegador ──► Controller ──► Service ──► Repository ──► Base de datos (H2 / PostgreSQL)
                 │              │
             plantillas     DTOs (UsuarioData, TareaData, EquipoData)
             Thymeleaf
```

| Paquete | Responsabilidad |
|---|---|
| `model` | Entidades JPA `Usuario`, `Tarea` y `Equipo`. `Usuario` y `Tarea` tienen una relación uno a muchos; `Tarea` tiene además un `comentario` opcional; `Usuario` y `Equipo`, una relación muchos a muchos. `Usuario` incluye los atributos `admin` y `bloqueado`. |
| `repository` | Interfaces Spring Data (`UsuarioRepository`, `TareaRepository`, `EquipoRepository`). |
| `service` | Lógica de negocio y transacciones (`UsuarioService`, `TareaService`, `EquipoService`). Devuelve DTOs, no entidades. `InitDbService` carga los datos iniciales en el perfil `dev`. |
| `dto` | Objetos de transferencia (`UsuarioData`, `TareaData`, `EquipoData`) y de formularios (`LoginData`, `RegistroData`). La conversión entre entidades y DTOs se hace con ModelMapper (`config/ModelMapperConfig`). |
| `controller` | Controladores Spring MVC que atienden las peticiones y eligen la plantilla. |
| `controller/exception` | Excepciones anotadas con `@ResponseStatus` que se traducen en códigos HTTP (401, 404). |
| `authentication` | `ManagerUserSession`: guarda en la sesión HTTP el id del usuario logeado. |

### Equipos

`Equipo` es el lado propietario de la relación muchos a muchos con `Usuario` (tabla `equipo_usuario`). JPA no actualiza los dos lados de la relación automáticamente, así que `Equipo.addUsuario` y `Equipo.removeUsuario` modifican ambas colecciones, y `EquipoService.eliminarEquipo` quita a todos los miembros antes de borrar el equipo. Eliminar un equipo nunca elimina a sus usuarios.

`EquipoService` lanza `EquipoServiceException` si el nombre del equipo es vacío, si el equipo o el usuario no existen, si se añade un usuario que ya pertenece al equipo o si se elimina uno que no pertenece.

### Autenticación y autorización

- Al hacer login, `UsuarioService.login` comprueba el email, la contraseña y si el usuario está bloqueado. Si todo es correcto, `ManagerUserSession` guarda el id del usuario en la sesión HTTP.
- Cada controlador comprueba el usuario de la sesión antes de atender la petición:
  - En las páginas de tareas y de su comentario, el usuario logeado debe ser el dueño de las tareas. Si no lo es, se devuelve 401.
  - En `/registrados`, `/registrados/{id}` y las acciones de bloqueo, el usuario logeado debe ser el administrador. Si no hay nadie logeado, se devuelve 401. Si el usuario no es administrador, se devuelve 401 con el mensaje "No tienes permisos suficientes para acceder a esta página".
  - En `/equipos/nuevo`, `/equipos/{id}/unirse` y `/equipos/{id}/salir` basta con estar logeado; en las rutas de edición y eliminación de equipos el usuario logeado debe ser el administrador. En ambos casos, sin sesión se devuelve 401.
- El administrador es redirigido al listado de usuarios al hacer login. El resto de usuarios va a su lista de tareas. Lo mismo ocurre justo después de registrarse, porque el registro deja al usuario logeado.

### Rutas principales

| Ruta | Descripción |
|---|---|
| `GET/POST /login`, `GET /logout` | Login y logout |
| `GET/POST /registro` | Registro de usuarios |
| `GET /about` | Página "acerca de" (versión y fecha de release) |
| `GET /usuarios/{id}/tareas` | Listado de tareas del usuario |
| `GET/POST /usuarios/{id}/tareas/nueva` | Crear tarea |
| `GET/POST /tareas/{id}/editar`, `DELETE /tareas/{id}` | Editar y borrar tarea |
| `GET /tareas/{id}` | Formulario para añadir o editar el comentario de la tarea |
| `POST /tareas/{id}/comentario`, `DELETE /tareas/{id}/comentario` | Guardar (añadir o sustituir) y borrar el comentario de la tarea |
| `GET /registrados` | Listado de usuarios (solo administrador) |
| `GET /registrados/{id}` | Descripción de un usuario (solo administrador) |
| `POST /registrados/{id}/bloquear`, `POST /registrados/{id}/habilitar` | Bloquear o habilitar a un usuario (solo administrador) |
| `GET /cuenta` | Datos del usuario logeado |
| `GET /equipos` | Listado de equipos |
| `GET /equipos/{id}` | Detalle de un equipo y sus miembros |
| `GET/POST /equipos/nuevo` | Crear equipo (el creador queda como miembro) |
| `POST /equipos/{id}/unirse`, `POST /equipos/{id}/salir` | Unirse a un equipo o salir de él |
| `GET/POST /equipos/{id}/editar` | Cambiar el nombre de un equipo (solo administrador) |
| `POST /equipos/{id}/eliminar` | Eliminar un equipo (solo administrador) |

### Vistas

Las plantillas Thymeleaf están en `src/main/resources/templates`. `fragments.html` contiene los fragmentos comunes: la cabecera, la barra de menú de Bootstrap 4 y los scripts. La barra cambia según haya o no un usuario logeado, y el enlace *Usuarios* solo se muestra al administrador. Los botones *Editar* y *Eliminar* de los equipos también son solo para el administrador.

## Enlaces

- [Tablero Trello](https://trello.com/b/OrjZJEnF/mads)
- [Repositorio GitHub](https://github.com/jpv49-ua/todolist-equipo-06)
