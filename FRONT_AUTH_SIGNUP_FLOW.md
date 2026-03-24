# Integracion Frontend: Signup con Verificacion de Correo

## Objetivo
Este documento describe el flujo que el frontend debe implementar para quedar alineado con el backend actual de `auth`.

Fecha de validacion del contrato: `2026-03-24`

## Resumen Ejecutivo
- El alta ya no es directa.
- El flujo obligatorio es:
  1. `POST /auth/signup/request-code`
  2. `POST /auth/signup/verify-code`
  3. `POST /auth/signup`
- El backend devuelve `errorCode` en todos los errores relevantes. El frontend debe tomar decisiones con `status + errorCode`, no con el texto libre del `message`.
- `POST /auth/login` tiene una particularidad:
  - en exito responde `LoginResponse` plano
  - en error responde `ApiResponse`
- El codigo de verificacion expira en `10 minutos`.
- El `verificationToken` expira en `15 minutos`.
- El backend permite hasta `5 intentos` de verificacion por codigo.
- El backend permite hasta `3 reenvios` sobre un codigo activo.
  - En la practica: `1 solicitud inicial + 3 reenvios` funcionan.
  - La siguiente solicitud devuelve `429 LIMITE_REENVIOS_ALCANZADO`.
- Hoy el backend no envia correo real por SMTP.
  - En pruebas locales puede devolver `data.codigoDebug` si el backend se levanta con `BANCUP_VERIFICATION_DEBUG_RETURN_CODE=true`.
  - El frontend no debe depender de `codigoDebug`.

## Regla Principal de UX
La pantalla de registro debe iniciar con solo el campo `correo` habilitado.

Antes de validar el correo:
- habilitado:
  - `correo`
  - boton `Enviar codigo`
- deshabilitado u oculto:
  - `codigo`
  - `usuario`
  - `contrasena`
  - `confirmarContrasena`
  - `genero`
  - boton final `Crear cuenta`

## Maquina de Estados Recomendada
Usar estos pasos de UI:

1. `email`
2. `code`
3. `signupForm`
4. `done`

Estado local minimo recomendado:

```ts
type SignupFlowState = {
  step: 'email' | 'code' | 'signupForm' | 'done';
  correo: string;
  codigo: string;
  usuario: string;
  contrasena: string;
  confirmarContrasena: string;
  genero: number | null;
  verificationToken: string | null;
  fechaExpiracionCodigo: string | null;
  isRequestingCode: boolean;
  isVerifyingCode: boolean;
  isSubmittingSignup: boolean;
  backendMessage: string | null;
  backendErrorCode: string | null;
};
```

## Flujo UI Recomendado

### Paso 1. Estado inicial
- Mostrar solo el input de `correo`.
- Deshabilitar u ocultar el resto del formulario.
- Boton: `Enviar codigo`.

### Paso 2. Enviar codigo
Accion:
- `POST /auth/signup/request-code`

Mientras la solicitud va en vuelo:
- deshabilitar `correo`
- deshabilitar `Enviar codigo`

Si responde `200`:
- mover a `step = code`
- conservar `correo`
- guardar `fechaExpiracionCodigo`
- limpiar errores anteriores
- habilitar:
  - input `codigo`
  - boton `Verificar codigo`
  - accion `Reenviar codigo`
  - accion `Cambiar correo`
- mantener deshabilitados:
  - `usuario`
  - `contrasena`
  - `confirmarContrasena`
  - `genero`
  - `Crear cuenta`

### Paso 3. Verificar codigo
Accion:
- `POST /auth/signup/verify-code`

Mientras la solicitud va en vuelo:
- deshabilitar `codigo`
- deshabilitar `Verificar codigo`

Si responde `200`:
- guardar `verificationToken`
- mover a `step = signupForm`
- mantener `correo` en modo solo lectura
- opcional:
  - ocultar el input `codigo`
  - o dejarlo visible pero bloqueado
- habilitar:
  - `usuario`
  - `contrasena`
  - `confirmarContrasena`
  - `genero`
  - `Crear cuenta`

### Paso 4. Crear cuenta
Accion:
- `POST /auth/signup`

Mientras la solicitud va en vuelo:
- deshabilitar todo el formulario

Si responde `201`:
- mover a `step = done`
- limpiar `verificationToken`
- mostrar mensaje de cuenta creada
- redirigir a login o hacer login manual despues

### Paso 5. Cambiar correo
Si el usuario cambia el correo en cualquier punto despues de `request-code`, el frontend debe resetear el flujo completo:

- `step = email`
- limpiar `codigo`
- limpiar `verificationToken`
- limpiar `usuario`
- limpiar `contrasena`
- limpiar `confirmarContrasena`
- limpiar `genero`
- limpiar `fechaExpiracionCodigo`

Esto es importante porque el `verificationToken` queda ligado al correo verificado.

## Formato de Respuestas

### Endpoints que responden con `ApiResponse`
- `POST /auth/signup/request-code`
- `POST /auth/signup/verify-code`
- `POST /auth/signup`

Formato exitoso:

```json
{
  "success": true,
  "message": "Mensaje",
  "data": {}
}
```

Formato de error:

```json
{
  "success": false,
  "message": "Mensaje",
  "errorCode": "CODIGO_ERROR"
}
```

### Endpoint `POST /auth/login`
En exito responde plano:

```json
{
  "success": true,
  "message": "Autenticacion exitosa",
  "token": "jwt",
  "nombre": "usuario"
}
```

En error responde `ApiResponse`:

```json
{
  "success": false,
  "message": "Correo o contrasena incorrectos",
  "errorCode": "CREDENCIALES_INVALIDAS"
}
```

## Contrato por Endpoint

### 1. `POST /auth/signup/request-code`

Request:

```json
{
  "correo": "usuario@correo.com"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "Codigo de verificacion generado",
  "data": {
    "correo": "usuario@correo.com",
    "fechaExpiracion": "2026-03-24T13:07:44.105643",
    "codigoDebug": "714248"
  }
}
```

Notas:
- `codigoDebug` solo aparece en ambiente debug.
- El frontend no debe mostrarlo ni depender de el en produccion.

Errores posibles:

| HTTP | errorCode | message exacto | Que debe hacer el frontend |
| --- | --- | --- | --- |
| 400 | `VALIDACION_FALLIDA` | `correo: El correo es obligatorio` | Quedarse en `email`, mostrar error inline en correo. |
| 400 | `VALIDACION_FALLIDA` | `correo: El formato del correo no es valido` | Quedarse en `email`, mostrar error inline en correo. |
| 400 | `VALIDACION_FALLIDA` | `El cuerpo de la peticion tiene un formato invalido.` | Mostrar error general y no avanzar. |
| 409 | `EMAIL_DUPLICADO` | `El correo 'usuario@correo.com' ya esta registrado` | Quedarse en `email`, sugerir ir a login. |
| 429 | `LIMITE_REENVIOS_ALCANZADO` | `Se alcanzo el limite de reenvios para este correo. Espera a que expire el codigo actual o intenta mas tarde.` | Mantener `step = code`, deshabilitar reenvio hasta que expire el codigo actual. |
| 500 | `ERROR_INTERNO` | `Error interno del servidor. Por favor intente de nuevo.` | Mostrar error general. |

### 2. `POST /auth/signup/verify-code`

Request:

```json
{
  "correo": "usuario@correo.com",
  "codigo": "123456"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "Correo verificado correctamente",
  "data": {
    "correo": "usuario@correo.com",
    "verificationToken": "jwt-temporal"
  }
}
```

Regla de frontend:
- Guardar `verificationToken` en memoria o `sessionStorage`.
- No guardarlo como token de sesion permanente.

Errores posibles:

| HTTP | errorCode | message exacto | Que debe hacer el frontend |
| --- | --- | --- | --- |
| 400 | `VALIDACION_FALLIDA` | `correo: El correo es obligatorio` | Quedarse en `code`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `correo: El formato del correo no es valido` | Quedarse en `code`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `codigo: El codigo es obligatorio` | Quedarse en `code`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `codigo: El codigo debe tener exactamente 6 digitos` | Quedarse en `code`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `El cuerpo de la peticion tiene un formato invalido.` | Quedarse en `code`, mostrar error general. |
| 400 | `CODIGO_VERIFICACION_INVALIDO` | `El codigo de verificacion es incorrecto.` | Mantener `step = code`, limpiar input de codigo o permitir correccion, no avanzar. |
| 400 | `CODIGO_VERIFICACION_INVALIDO` | `El codigo de verificacion es invalido o ya no esta disponible.` | Mantener `step = code` y forzar nueva solicitud de codigo. |
| 400 | `CODIGO_VERIFICACION_EXPIRADO` | `El codigo de verificacion expiro. Solicita uno nuevo.` | Mantener `step = code`, limpiar codigo y habilitar `Reenviar codigo`. |
| 429 | `LIMITE_INTENTOS_VERIFICACION_ALCANZADO` | `Se alcanzo el limite de intentos para este codigo. Solicita uno nuevo.` | Mantener `step = code`, deshabilitar `Verificar codigo` para ese codigo y pedir `Reenviar codigo`. |
| 500 | `ERROR_INTERNO` | `Error interno del servidor. Por favor intente de nuevo.` | Mostrar error general. |

Comportamiento validado:
- intentos 1 a 4 con codigo incorrecto: `400 CODIGO_VERIFICACION_INVALIDO`
- intento 5 y posteriores: `429 LIMITE_INTENTOS_VERIFICACION_ALCANZADO`

### 3. `POST /auth/signup`

Request:

```json
{
  "usuario": "front_flow_user",
  "correo": "usuario@correo.com",
  "contrasena": "Password123",
  "confirmarContrasena": "Password123",
  "genero": 1,
  "verificationToken": "jwt-temporal"
}
```

Success `201`:

```json
{
  "success": true,
  "message": "Usuario registrado exitosamente",
  "data": {
    "correo": "usuario@correo.com",
    "usuario": "front_flow_user",
    "rol": "CLIENTE"
  }
}
```

Errores posibles:

| HTTP | errorCode | message exacto | Que debe hacer el frontend |
| --- | --- | --- | --- |
| 400 | `VALIDACION_FALLIDA` | `usuario: El usuario es obligatorio` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `usuario: El usuario no debe exceder 100 caracteres` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `correo: El correo es obligatorio` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `correo: El formato del correo no es valido` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `contrasena: La contrasena es obligatoria` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `contrasena: La contrasena debe tener al menos 8 caracteres` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `La contrasena y su confirmacion no coinciden` | Mantener `step = signupForm`, mostrar error inline en confirmacion. |
| 400 | `VALIDACION_FALLIDA` | `genero: El genero es obligatorio` | Mantener `step = signupForm`, mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `verificationToken: El verificationToken es obligatorio` | El frontend no debe enviar signup sin token. Si ocurre, volver a verificar correo. |
| 400 | `VALIDACION_FALLIDA` | `El cuerpo de la peticion tiene un formato invalido.` | Mostrar error general. |
| 400 | `CORREO_NO_VERIFICADO` | `El correo aun no ha sido verificado para completar el registro.` | Regresar a `step = code`, limpiar `verificationToken`. |
| 400 | `CODIGO_VERIFICACION_EXPIRADO` | `La verificacion del correo expiro. Solicita un codigo nuevo.` | Regresar a `step = code`, limpiar `verificationToken`, pedir codigo nuevo. |
| 401 | `TOKEN_VERIFICACION_INVALIDO` | `El verificationToken es invalido o ya expiro. Verifica tu correo nuevamente.` | Regresar a `step = code`, limpiar `verificationToken` y volver a verificar. |
| 404 | `GENERO_NO_ENCONTRADO` | `El genero con id X no existe en la tabla CAT_GENERO` | Mostrar error de catalogo y no reenviar automaticamente. |
| 404 | `ROL_CLIENTE_NO_CONFIGURADO` | `El rol por defecto 'CLIENTE' no existe en la tabla ROLES...` | Error de backend/catalogo. Mostrar error general y escalar a backend. |
| 409 | `EMAIL_DUPLICADO` | `El correo 'usuario@correo.com' ya esta registrado` | Ofrecer ir a login. |
| 500 | `ERROR_INTERNO` | `Error interno del servidor. Por favor intente de nuevo.` | Mostrar error general. |

Notas importantes:
- Si el usuario vuelve a pedir codigo despues de haber verificado el correo, la verificacion anterior deja de servir.
- Si el frontend reutiliza un `verificationToken` viejo, el backend puede responder `TOKEN_VERIFICACION_INVALIDO` o `CORREO_NO_VERIFICADO`.

### 4. `POST /auth/login`

Request:

```json
{
  "correo": "usuario@correo.com",
  "contrasena": "Password123"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "Autenticacion exitosa",
  "token": "jwt",
  "nombre": "front_flow_user"
}
```

Errores posibles:

| HTTP | errorCode | message exacto | Que debe hacer el frontend |
| --- | --- | --- | --- |
| 400 | `VALIDACION_FALLIDA` | `correo: El correo es obligatorio` | Mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `correo: El formato del correo no es valido` | Mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `contrasena: La contrasena es obligatoria` | Mostrar error inline. |
| 400 | `VALIDACION_FALLIDA` | `El cuerpo de la peticion tiene un formato invalido.` | Mostrar error general. |
| 401 | `CREDENCIALES_INVALIDAS` | `Correo o contrasena incorrectos` | Mostrar error generico sin revelar si el correo existe. |
| 423 | `CUENTA_BLOQUEADA` | `Tu cuenta ha sido bloqueada temporalmente por multiples intentos fallidos. Contacta a soporte para desbloquearla.` | Deshabilitar nuevos intentos y mostrar flujo de soporte. |
| 500 | `ERROR_INTERNO` | `Error interno del servidor. Por favor intente de nuevo.` | Mostrar error general. |

Comportamiento validado:
- intentos 1 a 4 con password incorrecta: `401 CREDENCIALES_INVALIDAS`
- intento 5 y posteriores: `423 CUENTA_BLOQUEADA`

## Validaciones de Front Recomendadas
Aunque el backend ya valida todo, el frontend deberia validar antes de enviar:

- `correo`
  - obligatorio
  - formato email
- `codigo`
  - obligatorio
  - exactamente 6 digitos
- `usuario`
  - obligatorio
  - maximo 100 caracteres
- `contrasena`
  - obligatoria
  - minimo 8 caracteres
- `confirmarContrasena`
  - obligatoria
  - debe coincidir con `contrasena`
- `genero`
  - obligatorio

## Reglas de Comportamiento del Front

### Cuando mostrar cada control
- `correo`: siempre visible
- `codigo`: visible desde que `request-code` responde `200`
- formulario completo de alta: visible solo despues de `verify-code` con `200`

### Cuando limpiar estado
- si el usuario cambia `correo`
- si `verify-code` devuelve `CODIGO_VERIFICACION_INVALIDO` con mensaje `...ya no esta disponible`
- si `signup` devuelve `TOKEN_VERIFICACION_INVALIDO`
- si `signup` devuelve `CORREO_NO_VERIFICADO`
- si `signup` devuelve `CODIGO_VERIFICACION_EXPIRADO`

### Countdown recomendado
Usar `data.fechaExpiracion` de `request-code` para:
- mostrar cuenta regresiva del codigo
- saber hasta cuando conviene permitir reintento
- saber cuando reactivar `Reenviar codigo`

## Recomendacion de Manejo de Errores
No usar `message` para la logica.

Usar:
- `HTTP status`
- `errorCode`

Usar `message` solo para:
- texto visible al usuario
- logs de cliente

## Secuencia Recomendada

```text
Pantalla inicia
-> solo correo activo
-> request-code OK
-> habilitar codigo
-> verify-code OK
-> guardar verificationToken
-> habilitar formulario completo
-> signup OK
-> ir a login o iniciar sesion
```

## Ejemplo de Estrategia de UI

### Vista inicial
- campo: `correo`
- boton: `Enviar codigo`

### Vista codigo
- campo readonly: `correo`
- campo: `codigo`
- boton: `Verificar codigo`
- accion secundaria: `Reenviar codigo`
- accion secundaria: `Cambiar correo`

### Vista alta final
- campo readonly: `correo`
- campo: `usuario`
- campo: `contrasena`
- campo: `confirmarContrasena`
- selector: `genero`
- boton: `Crear cuenta`

## Observacion Importante para el Equipo Front
El contrato actual no es 100% homogeneo porque `login` no usa el mismo envelope de exito que `request-code`, `verify-code` y `signup`.

Mientras eso no cambie en backend, el frontend debe implementar:
- parser A para `login` exitoso
- parser B para errores de `login`
- parser C para respuestas `ApiResponse` del flujo de signup
