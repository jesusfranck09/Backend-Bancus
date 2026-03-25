# Session Notes

## Proyecto
- Ruta: `/Users/jesusfranciscofrancisco/Documents/dev_tools_base_ss`
- Rama actual: `main`
- Commit base para retomar: `ef23698`

## Qué se hizo
- Se renombro la carpeta del proyecto a `dev_tools_base_ss`.
- Se configuro Git localmente y se hizo push a `origin/main`.
- Se removio la creacion automatica de registros en `CUENTAS` durante `signup`.
- Se simplifico el contrato de auth:
  - `signup`: `usuario`, `correo`, `contrasena`, `confirmarContrasena`, `genero`
  - `login`: `correo`, `contrasena`
  - respuesta de `login`: `success`, `message`, `token`, `nombre`
- Se refactorizo el flujo de login:
  - validacion por correo
  - hash BCrypt
  - incremento de intentos fallidos
  - bloqueo de cuenta al llegar a 5 intentos
  - actualizacion de metadata de login
  - registro en auditoria
- Se ajustaron varios mapeos JPA para acercarlos al esquema nuevo de `APP_USER`:
  - `USUARIOS`
  - `ROLES`
  - `CAT_GENERO`
  - `AUDITORIA_EVENTOS`
- Se corrigio `AUDITORIA_EVENTOS` para usar `IP_ORIGEN` en vez de `CORREO`.
- Se implemento flujo previo de verificacion de correo para `signup` usando la tabla `CODIGOS_VERIFICACION_CORREO`.
- Se agregaron endpoints:
  - `POST /auth/signup/request-code`
  - `POST /auth/signup/verify-code`
- `POST /auth/signup` ahora requiere `verificationToken`.
- Se agrego `.gitignore` para excluir `target/`.

## Estado actual del codigo
- El proyecto compila con:
```bash
mvn -q -DskipTests compile
```
- El schema por defecto en `application.properties` quedo en:
```properties
spring.jpa.properties.hibernate.default_schema=APP_USER
```

## Hallazgos de base de datos
- Con credenciales anteriores (`APP_COREFINAN`) la conexion a Oracle si llego a abrir, pero el esquema visible no coincidia con las tablas esperadas.
- Se confirmo por imagen que las tablas relevantes viven bajo `APP_USER`.
- Con las credenciales probadas despues:
  - `DB_USER=APP_USER`
  - `DB_PASSWORD=Bancus_Proof#30`
  la app fallo con:
```text
ORA-01017: invalid username/password; logon denied
```
- Conclusion: para continuar hace falta el usuario/password correctos o confirmar si tambien cambia el alias/servicio del wallet.

## Verificacion 2026-03-24
- El usuario compartio nuevas credenciales para `APP_USER` y con ellas la conexion a Oracle si abrio correctamente usando el mismo wallet y alias `atpmariano_high`.
- La app se pudo levantar con:
```bash
DB_USER=APP_USER
WALLET_PATH=/Users/jesusfranciscofrancisco/Documents/dev_tools_base_ss/wallet
PORT=8081
mvn -q spring-boot:run
```
- `GET /auth/test-db` respondio HTTP 200.
- Se probo `POST /auth/login` con un correo inexistente y respondio HTTP 401 controlado:
```json
{"success":false,"message":"Correo o contrasena incorrectos"}
```
- En logs se confirmo que el flujo consulto:
```sql
from APP_USER.usuarios where correo=?
```
- Al probar `POST /auth/signup` se detecto un bug real de mapeo:
```text
ORA-00904: "CORREO": identificador no válido
```
- Se inspecciono el schema real y se confirmo que `APP_USER.AUDITORIA_EVENTOS` tiene:
  - `ID_EVENTO`
  - `ID_USUARIO`
  - `ACCION`
  - `IP_ORIGEN`
  - `FECHA_EVENTO`
- Se corrigio el codigo para persistir `ipOrigen` en auditoria tanto para `signup` como para `login`.
- Despues del fix:
  - `POST /auth/signup` respondio HTTP 201
  - `POST /auth/login` del usuario creado respondio HTTP 200
  - el rol asignado fue `CLIENTE`
  - `genero=1` es valido en `CAT_GENERO`
- Conclusion actual: conexion, `signup` y `login` ya funcionan contra `APP_USER`.

## Verificacion de correo 2026-03-24
- Se inspecciono la tabla real `CODIGOS_VERIFICACION_CORREO` y se confirmaron estas columnas:
  - `ID_CODIGO`
  - `CORREO`
  - `CODIGO_HASH`
  - `TIPO`
  - `ESTATUS`
  - `INTENTOS_FALLIDOS`
  - `REENVIOS`
  - `FECHA_EXPIRACION`
  - `FECHA_VERIFICADO`
  - `IP_ORIGEN`
  - `FECHA_CREA`
  - `USUARIO_CREA`
  - `FECHA_MODIFICA`
  - `USUARIO_MODIFICA`
- Se mapearon las entidades y repositorios para esa tabla.
- Flujo implementado:
  1. `POST /auth/signup/request-code`
  2. `POST /auth/signup/verify-code`
  3. `POST /auth/signup` con `verificationToken`
- `verify-code` devuelve un token temporal firmado para cerrar el alta de forma segura.
- `signup` ya no permite crear usuarios si el correo no fue verificado antes.
- Inicialmente la entrega del codigo se dejo con logging del backend y una opcion de debug response para pruebas locales.
- Pruebas reales sobre Oracle:
  - `request-code` respondio HTTP 200
  - `verify-code` respondio HTTP 200 con `verificationToken`
  - `signup` respondio HTTP 201
  - `login` del usuario creado respondio HTTP 200
  - un codigo incorrecto respondio `CODIGO_VERIFICACION_INVALIDO`

## Auditoria de respuestas 2026-03-24
- Se normalizaron respuestas de error para que tambien incluyan `errorCode` en casos de:
  - `CREDENCIALES_INVALIDAS`
  - `CUENTA_BLOQUEADA`
  - `USUARIO_NO_ENCONTRADO`
  - `TOKEN_EXPIRADO` / `TOKEN_INVALIDO` segun excepcion existente
- Se ajustaron las transacciones de `verify-code`, `signup` y `login` con `noRollbackFor = BancupException.class`.
- Motivo del ajuste:
  - antes, los incrementos de intentos fallidos se revertian al lanzar la excepcion
  - por eso el backend seguia respondiendo `CODIGO_VERIFICACION_INVALIDO` y no alcanzaba a persistir `LIMITE_INTENTOS_VERIFICACION_ALCANZADO`
  - el mismo problema afectaba el bloqueo real de cuenta en `login`
- Despues del fix y con pruebas reales sobre Oracle:
  - `verify-code` intento 1-4 incorrecto -> HTTP 400 `CODIGO_VERIFICACION_INVALIDO`
  - `verify-code` intento 5+ incorrecto -> HTTP 429 `LIMITE_INTENTOS_VERIFICACION_ALCANZADO`
  - `login` intento 1-4 incorrecto -> HTTP 401 `CREDENCIALES_INVALIDAS`
  - `login` intento 5+ incorrecto -> HTTP 423 `CUENTA_BLOQUEADA`
- Se genero documento para frontend en:
  - `FRONT_AUTH_SIGNUP_FLOW.md`
- Se genero especificacion OpenAPI para Swagger Editor en:
  - `auth-signup-flow.openapi.yaml`

## Tablas confirmadas por el usuario
- `AUDITORIA_EVENTOS`
- `CAT_GENERO`
- `DISPOSITIVOS`
- `ROLES`
- `USUARIOS`

## AWS Elastic Beanstalk 2026-03-24
- Se preparo un bundle para Elastic Beanstalk con:
  - `Procfile`
  - `bancup.jar`
  - carpeta `wallet/`
  - zip final `bancup-eb.zip`
- Se creo y despliego un environment Java SE en AWS Elastic Beanstalk:
  - app: `bancup-backend`
  - environment: `BackendBancup-env`
  - platform: `Corretto 21 running on 64bit Amazon Linux 2023`
- Se configuraron roles requeridos en IAM:
  - `aws-elasticbeanstalk-service-role`
  - `aws-elasticbeanstalk-ec2-role`
- Variables de entorno configuradas en Elastic Beanstalk:
  - `DB_USER=APP_USER`
  - `DB_PASSWORD` configurado en AWS
  - `JWT_SECRET` configurado en AWS
  - `WALLET_PATH=/var/app/current/wallet`
  - `PORT=5000`
- Hallazgo importante durante el despliegue:
  - la app arrancaba bien en `8080`
  - nginx de Elastic Beanstalk intentaba enrutar a `127.0.0.1:5000`
  - por eso devolvia `502 Bad Gateway`
  - se resolvio agregando `PORT=5000` en Elastic Beanstalk
- Logs confirmados en AWS:
  - Oracle conecto correctamente
  - el wallet fue encontrado correctamente en `/var/app/current/wallet`
  - Spring Boot arranco correctamente una vez alineado el puerto
- URL publica actual del backend:
  - `http://BackendBancup-env.eba-rmigtkqq.us-east-1.elasticbeanstalk.com`
- Verificaciones reales sobre la URL publica:
  - `GET /auth/test-db` -> HTTP 200
  - `POST /auth/signup/request-code` -> HTTP 200
- Para pruebas manuales en Postman/frontend se debe usar esta base URL:
  - `http://BackendBancup-env.eba-rmigtkqq.us-east-1.elasticbeanstalk.com`

## Correo SMTP y contrato request-code 2026-03-25
- Se agrego soporte de correo real por SMTP usando `spring-boot-starter-mail`.
- Se creo `SmtpVerificationCodeDeliveryService` para enviar el codigo al correo capturado cuando:
  - `BANCUP_MAIL_ENABLED=true`
  - existen variables `MAIL_*` configuradas
- Se dejo `LoggingVerificationCodeDeliveryService` como fallback cuando `BANCUP_MAIL_ENABLED=false` o no esta configurado.
- `POST /auth/signup/request-code` ahora puede devolver en `data`:
  - `codigo`
  - `codigoDebug`
  si `BANCUP_VERIFICATION_RETURN_CODE_IN_RESPONSE=true`
- En la implementacion actual, `codigo` y `codigoDebug` regresan el mismo valor cuando esa bandera esta activa.
- Se agrego el error estructurado `ENVIO_CORREO_FALLIDO` y se mapeo a HTTP 500 para fallos de configuracion o envio SMTP.
- Se mantuvo la validacion del codigo como string de exactamente `6` digitos.
- Se alineo la vigencia persistida despues de `verify-code` con los `15 minutos` del `verificationToken`, evitando que el `signup` falle antes de tiempo por la expiracion original del codigo.
- Se actualizo la documentacion de Swagger/OpenAPI para reflejar:
  - soporte SMTP
  - `data.codigo` en `request-code`
  - `data.verificationToken` en `verify-code`
  - `ENVIO_CORREO_FALLIDO`
- Pruebas automatizadas ejecutadas:
  - `mvn -q test`
  - cobertura agregada para validacion del codigo de `6` digitos
  - cobertura agregada para serializacion de `verificationToken`
  - cobertura agregada para `SmtpVerificationCodeDeliveryService`
- Prueba real local contra Oracle con correo por response en vez de SMTP:
  - `BANCUP_MAIL_ENABLED=false`
  - `BANCUP_VERIFICATION_RETURN_CODE_IN_RESPONSE=true`
  - `GET /auth/test-db` -> HTTP 200
  - `POST /auth/signup/request-code` -> HTTP 200 con `data.codigo` y `data.codigoDebug`
  - `POST /auth/signup/verify-code` -> HTTP 200 con `data.verificationToken`
  - `POST /auth/signup` -> HTTP 201
  - `POST /auth/login` -> HTTP 200
- Artefactos regenerados para despliegue:
  - `bancup.jar`
  - `bancup-eb.zip`
- Variables nuevas a configurar en Elastic Beanstalk para esta version si se habilita Gmail SMTP:
  - `BANCUP_MAIL_ENABLED=true`
  - `BANCUP_VERIFICATION_RETURN_CODE_IN_RESPONSE=true`
  - `MAIL_HOST=smtp.gmail.com`
  - `MAIL_PORT=587`
  - `MAIL_USERNAME` configurado en AWS
  - `MAIL_PASSWORD` configurado en AWS con App Password de Google
  - `MAIL_FROM` configurado en AWS
  - `MAIL_SMTP_AUTH=true`
  - `MAIL_SMTP_STARTTLS=true`
  - `MAIL_SMTP_STARTTLS_REQUIRED=true`

## CORS 2026-03-24
- Se actualizaron origenes permitidos en `CorsConfig.java` para frontend Amplify:
  - `https://bancus-dev.di0cd8gp9bhtr.amplifyapp.com`
  - `https://bancus-prod.di0cd8gp9bhtr.amplifyapp.com`
  - `https://bancus-integration.di0cd8gp9bhtr.amplifyapp.com`
- Importante:
  - CORS se configura por origen, no por path
  - por eso no se agrega `/login` ni `/signup`, solo el dominio base

## Estado git de referencia
- Ultimo commit empujado a `origin/main` al inicio de esta nota:
  - `ef23698` - `Add signup email verification flow and frontend docs`
- Archivos trabajados en esta sesion:
  - `auth-signup-flow.openapi.yaml`
  - `pom.xml`
  - `src/main/java/com/bancup/auth/dto/request/VerifySignupCodeRequest.java`
  - `src/main/java/com/bancup/auth/dto/response/RequestSignupCodeResponse.java`
  - `src/main/java/com/bancup/auth/service/impl/AuthServiceImpl.java`
  - `src/main/java/com/bancup/auth/service/impl/LoggingVerificationCodeDeliveryService.java`
  - `src/main/java/com/bancup/auth/service/impl/SmtpVerificationCodeDeliveryService.java`
  - `src/main/java/com/bancup/config/CorsConfig.java`
  - `src/main/java/com/bancup/exception/ErrorCode.java`
  - `src/main/java/com/bancup/exception/handler/GlobalExceptionHandler.java`
  - `src/main/resources/application.properties`
  - `src/test/java/com/bancup/BancupApplicationTests.java`
  - `src/test/java/com/bancup/auth/...`
  - `src/test/resources/...`
  - `Procfile`
  - `bancup.jar`
  - `bancup-eb.zip`
  - `SESSION.md`
- Nota:
  - `bancup.jar` y `bancup-eb.zip` son artefactos de despliegue local, no necesariamente conviene versionarlos en git

## Siguiente paso recomendado
1. Subir el `bancup-eb.zip` mas reciente a Elastic Beanstalk y aplicar variables `MAIL_*` / `BANCUP_MAIL_ENABLED` para habilitar correo real si se desea.
2. Validar desde el frontend Amplify el flujo completo contra la URL publica de Elastic Beanstalk:
   - pedir codigo
   - confirmar si el frontend usara `data.codigo` mientras se integra el envio real por correo
   - validar codigo
   - guardar `verificationToken`
   - enviar `verificationToken` en `signup`
   - probar tambien `login`
3. Decidir si `BANCUP_VERIFICATION_RETURN_CODE_IN_RESPONSE` se queda activo solo para integracion o tambien para QA.
4. Decidir si el backend se quedara en Elastic Beanstalk o luego se migrara a otro runtime de AWS.
5. Definir si los usuarios de prueba creados se conservan o se limpian despues por SQL/manualmente.

## Cómo retomar mañana
Decir en Codex:
```text
lee SESSION.md y retoma desde el ultimo commit disponible en main, considerando los cambios de SMTP, CORS y despliegue Elastic Beanstalk
```
