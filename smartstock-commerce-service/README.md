# SmartStock Commerce Service

Microservicio para la gestión de comercios dentro del ecosistema SmartStock. Proporciona una arquitectura multi-tenant para administrar la información de los comercios participantes.

## Stack Tecnológico

- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.4.1
- **Gestor de Dependencias:** Maven
- **Persistencia:** Spring Data JPA / Hibernate
- **Base de Datos:** Oracle Database (ojdbc11)
- **Seguridad:** Spring Security con JWT (jjwt)
- **Documentación:** SpringDoc OpenAPI (Swagger UI)
- **Monitoreo:** Spring Boot Actuator

## Requisitos

- Java 17+
- Maven 3.6+
- Instancia de Oracle Database (o Docker para ejecutar una)
- Wallet de Oracle (si se conecta a una instancia en la nube como Autonomous Database) en `src/main/resources/Wallet_SmartStock`

## Configuración y Ejecución

### Local (Maven)

1. Clonar el repositorio.
2. Configurar las variables de entorno o actualizar `src/main/resources/application.properties`.
3. Compilar el proyecto:
   ```bash
   mvn clean install
   ```
4. Ejecutar la aplicación:
   ```bash
   mvn spring-boot:run
   ```
   La aplicación estará disponible en `http://localhost:8085`.

### Docker

1. Construir la imagen:
   ```bash
   docker build -t smartstock-commerce-service .
   ```
2. Ejecutar el contenedor:
   ```bash
   docker run -p 8085:8085 --env-file .env smartstock-commerce-service
   ```

## Scripts Disponibles

- `mvn clean package`: Compila y genera el archivo JAR.
- `mvn test`: Ejecuta las pruebas unitarias e integración.
- `mvn spring-boot:run`: Ejecuta la aplicación localmente.
- `mvn dependency:go-offline`: Descarga todas las dependencias necesarias.

## Variables de Entorno

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `SPRING_DATASOURCE_URL` | URL de conexión JDBC a Oracle | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `smartstock` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | `smartstock` |
| `SMARTSTOCK_JWT_SECRET` | Clave secreta para firmar tokens JWT | (valor por defecto en properties) |
| `TNS_ADMIN` | (Solo Docker) Ruta a la Wallet de Oracle | `/app/wallet` |

## Pruebas

Para ejecutar las pruebas del sistema:
```bash
mvn test
```
Las pruebas se encuentran en `src/test/java` e incluyen:
- `ComercioServiceTest`: Pruebas de lógica de negocio.
- `ComercioControllerTest`: Pruebas de integración de endpoints.
- `JwtUtilsTest`: Pruebas de utilidades de seguridad.

## Estructura del Proyecto

```text
smartstock-commerce-service/
├── src/main/java/com/osanzana/smartstock/commerce/
│   ├── CommerceApplication.java       # Punto de entrada de la aplicación
│   ├── core/                          # Entidades y Repositorios base
│   ├── gestion/                       # Lógica de negocio (Services, Controllers)
│   └── shared/                        # DTOs, Excepciones, Seguridad
├── src/main/resources/
│   ├── Wallet_SmartStock/             # Credenciales de Oracle Wallet
│   ├── application.properties         # Configuración base
│   └── application-prod.properties    # Configuración de producción
├── Dockerfile                         # Configuración de contenedorización
└── pom.xml                            # Configuración de Maven
```

## API Endpoints

- **Swagger UI:** `http://localhost:8085/swagger-ui.html`
- **Actuator Health:** `http://localhost:8085/actuator/health`
- **Comercios API:** `/api/v1/comercios`

## TODOs / Pendientes

- [ ] Implementar auditoría de cambios en comercios.
- [ ] Configurar CI/CD para despliegue automático.
- [ ] Añadir más pruebas de integración con base de datos real.

## Licencia

TODO: Definir licencia del proyecto.
