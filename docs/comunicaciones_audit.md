# Sistema de Correos y Notificaciones — Arquitectura Real

Este documento reemplaza la versión anterior, que describía una arquitectura aspiracional nunca implementada (mencionaba un proveedor "Resend" inexistente en el código, un scheduler cada 5 segundos que no corresponde a ningún job real, y una única tabla `envio_email` como mecanismo universal). Lo que sigue describe el sistema tal como quedó tras el cierre de la auditoría de notificaciones y correos (Fases 1 a 11).

Hay **dos subsistemas de correo independientes**, con tablas y schedulers propios:

1. **Notificaciones transaccionales** (`notificacion` / `notificacion_entrega`): eventos de negocio individuales (una reserva, un cambio de contraseña, un cierre de caja con discrepancia). Procesadas por `NotificacionEntregaEmailScheduler`.
2. **Campañas de marketing masivo** (`campana_email` / `envio_email`): envíos programados a segmentos de clientes. Procesadas por `EnvioEmailScheduler`. No comparte tablas ni colas con el subsistema transaccional.

---

## 1. Notificaciones transaccionales — Transactional Outbox

### 1.1 Tablas

- **`notificacion`**: el hecho de negocio. Campos relevantes: `tipo_codigo` (FK a `tipo_notificacion`), `destinatario_usuario_id` (staff, UUID) o `destinatario_cliente_id` (cliente, Long) — mutuamente excluyentes, `entidad_tipo`/`entidad_id` (vínculo genérico con la entidad que originó la notificación, ej. `entidad_tipo='reserva_publica'`), `titulo`/`mensaje` ya interpolados en el momento de creación, `prioridad`, `leida`/`leida_at`, `expira_at`.
- **`notificacion_entrega`**: una fila por canal de entrega (`IN_APP` o `EMAIL`) de cada `notificacion`. Campos: `canal`, `estado` (string libre, sin enum en Java; valores usados por el código: `PENDIENTE`, `ENVIADO`, `ERROR`, `REBOTADO`), `intentos`, `enviado_at`, `mensaje_error`, `proveedor_id`.

Las entregas `IN_APP` se marcan `ENVIADO` de inmediato en el momento de creación (no hay transporte real; el registro en sí es la notificación que se lista en el feed). Las entregas `EMAIL` nacen `PENDIENTE` y son recogidas por el scheduler.

### 1.2 Catálogo `tipo_notificacion`

Tabla de configuración que gobierna cómo se comporta cada tipo de evento: `canales_default` (array, ej. `['IN_APP','EMAIL']`), `plantilla_titulo`/`plantilla_mensaje` (con placeholders `{clave}` interpolados contra el mapa `datosExtra` del comando de creación), `prioridad` (afecta el TTL de la notificación: BAJA=7 días, NORMAL=30, ALTA=60, CRITICA=90), y `es_obligatoria` — si es `true`, la entrega por `EMAIL` se genera sin importar la preferencia del destinatario (ver 1.4).

Algunos tipos solo tienen `IN_APP` en `canales_default` (ej. `CAJA_APERTURA`, `CAJA_MOVIMIENTO_GRANDE`, `RESERVA_INGRESO_CONFIRMADO`) — son de bajo ruido y deliberadamente no generan correo.

### 1.3 Creación de notificaciones

`CrearNotificacionPort` (implementado por `NotificacionService`) expone dos rutas:

- **`notificarTransaccional(cmd)`**: síncrona, se ejecuta dentro de la misma transacción del caso de uso que la invoca. Se usa cuando la notificación es parte del contrato de la operación (ej. avisar al cliente que su correo de contacto cambió).
- **`notificar(cmd)`**: asíncrona (`@Async`), de mejor esfuerzo — cualquier excepción se registra en log y no se propaga. Se usa para notificaciones no críticas donde un fallo de notificación no debe abortar la operación de negocio (ej. alertar a administradores de un movimiento de caja elevado).

### 1.4 Preferencias de notificación

`PreferenciaUsuario` (tabla propia, clave `usuario_id` UUID) permite a **staff** desactivar el canal `EMAIL` para notificaciones no obligatorias. Los **clientes** (`destinatario_cliente_id`) no tienen un mecanismo de preferencia equivalente — siempre reciben `EMAIL` si el tipo lo incluye en `canales_default`, independientemente de `es_obligatoria`. `aceptaComunicaciones` en `ClientePerfil` es un concepto distinto (consentimiento de marketing), no gobierna las notificaciones transaccionales.

### 1.5 Procesamiento asíncrono — `NotificacionEntregaEmailScheduler`

- `procesarPendientes()`: cada 30 segundos (`fixedDelay = 30_000`), procesa lotes de hasta 50 entregas `EMAIL` en estado `PENDIENTE`.
- `reintentarFallidos()`: diariamente a las 3:15am (America/Lima), reintenta entregas en `ERROR` con menos de 3 intentos.
- Por cada entrega: resuelve la `Notificacion` asociada, resuelve un renderizador registrado para su `tipoCodigo` (ver 1.6), renderiza el contenido y envía vía `JavaMailCorreoClient` (SMTP real a través de `JavaMailSender` de Spring, no un proveedor de terceros tipo Resend).
- Éxito → `ENVIADO`. Fallo transitorio (excepción al enviar) → `ERROR` si quedan reintentos, `REBOTADO` si se agotaron los 3 intentos. Fallos no recuperables (notificación eliminada, sin renderizador configurado, destinatario sin correo registrado) → `REBOTADO` inmediato, sin reintentos.
- Cuando una entrega llega a `REBOTADO`, se dispara `notificar()` con `tipoCodigo=ERROR_ENVIO_EMAIL` hacia todos los administradores activos (`ResolverAdministradoresPort`), para que el fallo de envío sea visible operativamente sin depender de que el cliente lo reporte.

### 1.6 Renderizadores de correo transaccional

Cada tipo de notificación que debe generar correo implementa `RenderizadorCorreoTransaccional` (`tipoCodigo()` + `renderizar(Notificacion): ContenidoCorreo`) y se auto-registra en `RenderizadorCorreoRegistry` (Spring inyecta la lista completa de beans del tipo; el registro se indexa por `tipoCodigo()`). Inventario real de renderizadores wireados a la fecha de este documento:

| tipoCodigo | Renderizador | Plantilla |
|---|---|---|
| `TICKET_DISPONIBLE` | `RenderizadorReservaPendiente` | `email-reserva-pendiente.html` |
| `PAGO_CONFIRMADO` | `RenderizadorTicketConfirmado` | `email-ticket.html` (incluye PDF adjunto del ticket) |
| `PAGO_RECHAZADO` | `RenderizadorReservaRechazada` | `email-reserva-rechazada.html` |
| `RESERVA_CANCELADA` | `RenderizadorReservaCancelada` | `email-reserva-cancelada.html` |
| `RESERVA_RECORDATORIO` | `RenderizadorReservaRecordatorio` | `email-reserva-recordatorio.html` |
| `RESERVA_REPROGRAMADA` | `RenderizadorReservaReprogramada` | `email-reserva-reprogramada.html` |
| `EVENTO_PRESUPUESTO_ENVIADO` | `RenderizadorSolicitudEvento` | `email-evento-solicitud.html` |
| `EVENTO_CONFIRMADO` | `RenderizadorEventoConfirmado` | `email-evento-confirmado.html` |
| `EVENTO_ABONO_RECIBIDO` | `RenderizadorAbonoEvento` | `email-evento-abono.html` |
| `EVENTO_CANCELADO_ADMIN` | `RenderizadorEventoCancelado` | `email-evento-cancelado.html` |
| `EVENTO_RECORDATORIO_3DIAS` | `RenderizadorEventoRecordatorio3Dias` | `email-evento-recordatorio.html` |
| `DOCUMENTO_LISTO` | `RenderizadorComprobanteVenta` | `email-venta.html` |
| `USUARIO_ACTIVACION` | `RenderizadorBienvenidaStaff` | `welcome-user.html` |
| `CAMBIO_PASSWORD` | `RenderizadorCambioPassword` | `email-aviso-seguridad.html` |
| `USUARIO_BLOQUEADO` | `RenderizadorUsuarioBloqueado` | `email-aviso-seguridad.html` |
| `USUARIO_DESBLOQUEADO` | `RenderizadorUsuarioDesbloqueado` | `email-aviso-seguridad.html` |
| `CAMBIO_ROL` | `RenderizadorCambioRolStaff` | `email-aviso-seguridad.html` |
| `CAMBIO_CORREO_SOLICITADO` | `RenderizadorCambioCorreoSolicitado` | `email-cambio-correo-solicitado.html` |
| `CAMBIO_CORREO_ALERTA` | `RenderizadorCambioCorreoAlerta` | `email-aviso-seguridad.html` |
| `CAMBIO_CORREO_CONFIRMADO` | `RenderizadorCambioCorreoConfirmado` | `email-aviso-seguridad.html` |
| `CAJA_CIERRE_DISCREPANCIA` | `RenderizadorCajaCierreDiscrepancia` | `email-aviso-seguridad.html` |

Los seis tipos de avisos de seguridad de cuenta comparten una única plantilla genérica (`email-aviso-seguridad.html`) porque `notificacion.titulo`/`notificacion.mensaje` ya llegan completamente interpolados desde `NotificacionService.persistir()` — no requieren maquetado propio por tipo.

Cualquier `tipoCodigo` sin renderizador registrado que tenga `EMAIL` en `canales_default` fallará como no recuperable (`REBOTADO` inmediato) la primera vez que el scheduler lo procese — es responsabilidad de quien agregue un nuevo tipo de notificación con canal `EMAIL` crear su renderizador correspondiente.

---

## 2. Campañas de marketing masivo

Módulo independiente (`CampanaEmail`, `PlantillaEmail`, `EnvioEmail`), pensado para envíos a segmentos de clientes (VIP, frecuentes, nuevos, inactivos, corporativos, etc.), no para eventos transaccionales individuales.

- `EnvioEmailScheduler.procesarEnviosPendientes()`: cada 60 segundos, toma campañas `PROGRAMADA` cuya fecha llegó y las pasa a `ENVIANDO`; procesa lotes de 50 envíos `PENDIENTE` por campaña en cada corrida hasta que no queden pendientes, momento en el que la campaña pasa a `FINALIZADA`.
- `EnvioEmailScheduler.reintentarFallidos()`: diariamente a las 3:00am (America/Lima), reintenta envíos con menos de 3 intentos.
- El conteo de destinatarios de una campaña (`GET /marketing/campanas/{id}/destinatarios/count`) reutiliza exactamente la misma lógica de filtrado que el envío real (método privado compartido en `MarketingService`), para que el número mostrado antes de confirmar el envío nunca diverja del envío efectivo.
- No genera adjuntos PDF ni depende de las tablas `notificacion`/`notificacion_entrega`.

---

## 3. Tokens de un solo uso

Dos tablas de tokens hasheados (SHA-256, vía `TokenHasher`; el valor en texto plano nunca se persiste, solo viaja una vez por correo):

- **`staff_token`**: activación de cuenta y restablecimiento de contraseña para personal interno.
- **`cliente_token`** (tipo `VERIFICAR_CORREO`): confirmación de cambio del correo de contacto del cliente (`ClientePerfil.correo`). No debe confundirse con el correo de acceso a Supabase Auth (`perfil_usuario.correo`); no existe en el backend un mecanismo para sincronizar ambos campos — son conceptos distintos (contacto vs. credencial de login).

---

## 4. Estado de entrega expuesto al frontend

`GET /api/v1/reservas/{idReserva}/estado-correo` resuelve la `notificacion` más reciente ligada a `entidad_tipo='reserva_publica'` y agrega el estado de su entrega `EMAIL` en un contrato estable de 4 valores: `ENVIADO | PENDIENTE | ERROR | SIN_ENVIO` (este último cuando el tipo de notificación no generó entrega por ese canal). La pantalla de confirmación de reserva (`SuccessStep.tsx`) lo consulta de forma no bloqueante y solo cambia su mensaje por defecto cuando el estado es `ERROR`, ofreciendo la descarga manual del ticket en PDF como alternativa.

Los flujos que dependen enteramente de Supabase Auth (recuperación de contraseña, registro, restablecimiento de contraseña por un administrador) no tienen outbox propio — Supabase gestiona el envío internamente y el backend no puede confirmar la entrega. La UI de esos flujos comunica explícitamente "solicitud procesada", no "correo entregado".

---

## 5. Observabilidad

Instrumentación con Micrometer (vía `spring-boot-starter-actuator`, ya presente en el proyecto; expuesto en `/actuator/metrics` — `management.endpoints.web.exposure.include: health,info,metrics`):

- **Contadores** (por lote procesado):
  - `notificacion.email.enviados` / `notificacion.email.fallidos`, con tag `tipo` = `tipoCodigo` de la notificación.
  - `marketing.email.enviados` / `marketing.email.fallidos` (sin tag por campaña, para evitar cardinalidad no acotada).
- **Timers**: `notificacion.email.lote.duracion` y `marketing.email.lote.duracion`, medidos alrededor de cada corrida de scheduler.
- **Alerta operativa**: al final de cada lote, si el número de entregas procesadas alcanza el mínimo configurado (`playzone.correo.alerta.minimo-muestra`, por defecto 5) y la tasa de fallo supera el umbral configurado (`playzone.correo.alerta.tasa-fallo-umbral`, por defecto `0.2` = 20%), se emite un log `WARN` con el detalle del lote. Ambos umbrales son configurables por variable de entorno (`CORREO_ALERTA_TASA_FALLO`, `CORREO_ALERTA_MINIMO_MUESTRA`) sin requerir redeploy de código.

Consulta rápida de la tasa de éxito/fallo reciente por tipo de notificación:

```
GET /actuator/metrics/notificacion.email.enviados?tag=tipo:PAGO_CONFIRMADO
GET /actuator/metrics/notificacion.email.fallidos?tag=tipo:PAGO_CONFIRMADO
```

---

## 6. Referencia

El detalle de cada fase de implementación (decisiones tomadas, hallazgos descubiertos durante el desarrollo, y cobertura de tests) está documentado en `_analisis/plan-implementacion-notificaciones-y-correos.md`.
