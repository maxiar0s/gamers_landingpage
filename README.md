# GG Comunidad — eventos gamer

Aplicación web en español para descubrir festivales, torneos y encuentros gamer. Spring Boot renderiza páginas Thymeleaf y consulta el catálogo desde MySQL mediante JDBC. La búsqueda pública y sus cinco filtros, las selecciones editoriales y las fichas protegidas leen de la tabla `eventos`.

## Levantar con Docker Compose

Requisitos: Docker Engine/Desktop con Compose v2.

Desde la raíz del proyecto:

```sh
docker compose up --build
```

El frontend Nginx escucha en todas las interfaces de WSL por defecto y reenvía las solicitudes al backend Spring. Abre <http://localhost:8080/eventos>; si Windows no redirige localhost hacia WSL, ejecuta `hostname -I` en Ubuntu y abre `http://<IP-de-WSL>:8080/eventos` desde Windows. La IP puede cambiar al reiniciar WSL. Esta configuración puede exponer el sitio a otras redes alcanzables: úsala solo en un entorno de confianza y no publiques las credenciales de demostración. Para limitarlo nuevamente a WSL, ejecuta `FRONTEND_BIND_ADDRESS=127.0.0.1 docker compose up -d`. Para cambiar el puerto, define `FRONTEND_PORT`, por ejemplo `FRONTEND_PORT=8081 docker compose up -d`.

| Servicio | Función | Exposición |
| --- | --- | --- |
| `frontend` | Nginx como proxy inverso hacia Spring | Todas las interfaces de WSL, puerto `8080` por defecto; configurable con `FRONTEND_BIND_ADDRESS` |
| `backend` | Aplicación Spring Boot/Thymeleaf, compilada con Java 17 y Maven | Solo red interna de Compose |
| `database` | MySQL 8.0; ejecuta `database/eventos.sql` al inicializar una base vacía | Solo red interna de datos |

Compose conserva el volumen persistente `mysql_data`; no borres ni reinicialices el volumen para aplicar la integración. `database/eventos.sql` se ejecuta solo al inicializar un directorio de datos vacío. Si la base existente ya tiene `comunidad_gamer.eventos`, la aplicación la lee directamente y mantiene sus filas. No se expone ningún puerto de MySQL: solo el backend accede a la red privada de datos.

Spring recibe `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` desde Compose. El usuario de aplicación `eventos_dev` es distinto de root y está limitado a la base configurada por MySQL. `MYSQL_PASSWORD` debe coincidir con la contraseña de ese usuario en la base persistente. En una instalación nueva, Compose permite configurar `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD` en el entorno; los valores predeterminados son únicamente de desarrollo local y no aptos para producción. Un volumen ya inicializado conserva las cuentas existentes: cambiar variables no rota sus contraseñas. No publiques contraseñas ni las incluyas en logs. No expongas los puertos privados de backend o base de datos.

Para revisar la configuración efectiva sin iniciar contenedores:

```sh
docker compose config
```

## Ejecución local sin Docker

Requisitos: Java 17 y Maven 3.8+.

```powershell
mvn.cmd spring-boot:run
```

Abre <http://localhost:8080/eventos>. Para ejecutar las pruebas automatizadas:

```powershell
mvn.cmd test
```

## Usuarios de demostración

| Usuario | Contraseña | Perfil |
| --- | --- | --- |
| `player` | `pixel123` | Gamer |
| `organizador` | `arena123` | Gamer y organizador |
| `invitado` | `retro123` | Gamer |

Las cuentas se almacenan solo en memoria al iniciar la aplicación; cada contraseña se codifica con BCrypt. Estas credenciales públicas son únicamente para uso local de demostración: no deben reutilizarse en un entorno real.

## Catálogo y datos

La tabla `comunidad_gamer.eventos` alimenta la agenda pública, búsqueda y detalle. Las consultas de búsqueda usan parámetros JDBC; valores como `q`, categoría, juego, país y experiencia no se interpolan en SQL. Si la tabla está vacía, la agenda y las selecciones editoriales aparecen vacías sin fallar; una ficha inexistente devuelve 404.

`database/eventos.sql` crea la tabla e inserta/actualiza cuatro filas solo en la inicialización de un volumen nuevo. Para una base existente, conserva la tabla y sus registros: revisa/aplica cambios de esquema de forma controlada si fueran necesarios, sin restablecer el volumen. Las pruebas automatizadas usan una base H2 aislada en modo compatible con MySQL y no escriben en la base de Compose.

## Rutas

- `GET /` y `GET /eventos`: agenda pública. Filtros: `q`, `categoria`, `juego`, `pais`, `experiencia`.
- `GET /eventos/{slug}`: ficha detallada protegida por autenticación.
- `GET /login`: acceso de usuario; Spring Security gestiona autenticación y tokens CSRF.

Las fechas y ubicaciones son contenido de muestra; confirma los datos con cada organización.
