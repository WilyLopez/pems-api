# Documentacion del Sistema de Correos y Notificaciones

Esta documentacion describe el comportamiento, eventos disparadores, plantillas HTML asociadas, notificaciones in-app y el flujo tecnico de procesamiento del sistema de comunicaciones de la plataforma.

---

## 1. Flujo de Reservas Publicas (Clientes Externos)

Las reservas publicas (entradas al parque por horas o dias) disparan comunicaciones automaticas basadas en el cambio de estado del pago y de la reserva.

### A. Registro de Reserva (Pago Pendiente de Validacion)
* **Disparador:** El cliente realiza una reserva desde la web y sube su comprobante de pago (Yape / Transferencia).
* **Correo Automatico:**
  * **Plantilla:** `email-reserva-pendiente.html`
  * **Asunto:** Solicitud de Reserva Recibida - Pendiente de Validacion
  * **Contenido:** Confirma la recepcion de la solicitud y del comprobante. Aclara explicitamente que la reserva **no esta confirmada** y que debera esperar la aprobacion de un administrador antes de asistir a la sede.
* **Notificacion In-App:**
  * **Cliente:** Mensaje informando que su comprobante de pago esta en proceso de revision.
  * **Administrador:** Alerta en el panel administrativo indicando que existe una nueva reserva con pago pendiente de validacion.

### B. Confirmacion de Pago (Aprobacion Administrativa)
* **Disparador:** El administrador valida el comprobante en el panel y marca la reserva como confirmada (`confirmarPago`).
* **Correo Automatico:**
  * **Plantilla:** `email-reserva-confirmada` (o renderizado del ticket definitivo).
  * **Asunto:** Confirmacion de Reserva - Ticket de Ingreso
  * **Adjunto:** Ticket digital en formato PDF generado de forma dinamica en caliente (contiene detalles de la reserva y el codigo QR de ingreso).
  * **Contenido:** Instrucciones de ingreso a la sede y bienvenida.
* **Notificacion In-App:**
  * **Cliente:** Notificacion de exito con enlace directo para descargar el ticket en PDF en formato digital.

### C. Rechazo de Pago (Comprobante Invalido)
* **Disparador:** El administrador rechaza el comprobante de pago indicando un motivo (ej. imagen borrosa, monto incorrecto) a traves de `rechazarPago`.
* **Correo Automatico:**
  * **Plantilla:** `email-reserva-rechazada.html`
  * **Asunto:** Observacion en su Comprobante de Pago - Reserva Pendiente
  * **Contenido:** Explica que el comprobante fue rechazado, muestra el motivo ingresado por el administrador y proporciona un enlace seguro para que el usuario vuelva a cargar un comprobante valido.
* **Notificacion In-App:**
  * **Cliente:** Alerta critica solicitando la actualizacion del comprobante de pago de la reserva.

### D. Cancelacion de Reserva
* **Disparador:** La reserva es cancelada por el cliente o administrativamente por falta de pago o reprogramacion fallida.
* **Correo Automatico:**
  * **Plantilla:** `email-reserva-cancelada.html`
  * **Asunto:** Cancelacion de Reserva
  * **Contenido:** Notifica la cancelacion formal de la reserva y detalla el motivo de la cancelacion.
* **Notificacion In-App:**
  * **Cliente:** Mensaje confirmando la anulacion de la reserva.

---

## 2. Flujo de Eventos Privados (Cumpleaños y Celebraciones)

Los eventos privados tienen flujos de pago por cuotas y aprobaciones contractuales especificas.

### A. Registro de Solicitud de Cotizacion
* **Disparador:** Un cliente solicita informacion para reservar un cumpleaños o evento privado.
* **Correo Automatico:**
  * **Al Cliente (Plantilla `email-evento-solicitud.html`):** Agradecimiento por su interes e informacion de que un asesor se pondra en contacto.
  * **Al Administrador (Plantilla `email-evento-admin-alerta.html`):** Alerta inmediata con los datos de contacto y detalles del evento solicitado para su cotizacion rapida.
* **Notificacion In-App:**
  * **Administrador:** Alerta en la bandeja de eventos pendientes indicando nueva solicitud de cotizacion.

### B. Confirmacion de Evento Privado
* **Disparador:** Se llega a un acuerdo con el cliente y se aprueba la fecha del evento.
* **Correo Automatico:**
  * **Plantilla:** `email-evento-confirmado.html`
  * **Asunto:** Confirmacion de Evento Privado - Contrato Disponible
  * **Contenido:** Carta de bienvenida, confirmacion de la fecha reservada e instrucciones para la firma digital del contrato.
* **Notificacion In-App:**
  * **Cliente:** Enlace para revisar y firmar el contrato del evento en el portal.

### C. Registro de Abonos (Pagos de Cuotas)
* **Disparador:** El cliente realiza un abono a cuenta del evento privado y este es registrado en el sistema.
* **Correo Automatico:**
  * **Plantilla:** `email-evento-abono.html`
  * **Asunto:** Confirmacion de Abono - Estado de Cuenta del Evento
  * **Contenido:** Detalle del abono recibido (monto y medio de pago), monto total acumulado abonado a la fecha y desglose claro del saldo restante pendiente de pago.
* **Notificacion In-App:**
  * **Cliente:** Mensaje confirmando la recepcion del abono y actualizacion automatica de su estado de cuenta en el portal.

### D. Cancelacion de Evento Privado
* **Disparador:** Se cancela el evento de forma definitiva.
* **Correo Automatico:**
  * **Plantilla:** `email-evento-cancelado.html`
  * **Asunto:** Cancelacion de Evento Privado
  * **Contenido:** Notifica la anulacion del evento y las condiciones de devolucion o penalidad de acuerdo con el contrato.

---

## 3. Arquitectura del Transactional Outbox (Procesamiento Asincrono)

Para garantizar un rendimiento optimo del servidor (tiempos de respuesta de las APIs menores a 50ms) y evitar bloqueos por latencia de la red con el servidor de correo SMTP, se implemento el patron **Transactional Outbox**.

```
+------------------------------------+
| Acción del Usuario (Crear Reserva) |
+------------------------------------+
                  | (Menos de 50ms)
                  v
+------------------------------------+
| Guardar Registro en Base de Datos  |
|   - Reserva                        |
|   - Outbox: EnvioEmail (PENDIENTE) |
+------------------------------------+
                  |
                  | (Scheduler asíncrono cada 5 segundos)
                  v
+------------------------------------+
|       EnvioEmailScheduler          |
|  1. Busca registros PENDIENTES     |
|  2. Deserializa metadata (JSON)    |
|  3. Renderiza plantilla HTML       |
|  4. Genera PDFs en memoria         |
|  5. Transmite email vía Resend     |
|  6. Actualiza Outbox (ENVIADO)     |
+------------------------------------+
```

### Flujo Tecnico de Envio
1. **Creacion:** El adaptador de correo no realiza conexiones SMTP ni compila archivos PDF. En su lugar, serializa los parametros de la notificacion (IDs, nombres, montos) en un campo `metadata` en formato JSON y guarda un registro en la tabla `envio_email` con estado `PENDIENTE`.
2. **Procesamiento:** `EnvioEmailScheduler` consulta la tabla cada 5 segundos. Recupera los pendientes, extrae la `metadata`, genera en memoria los PDFs de tickets o notas de venta a partir de los datos frescos de la BD y los envia via SMTP.
3. **Liberacion:** Los archivos adjuntos PDF se manejan como flujos de bytes en memoria volatil. Una vez enviado el correo electronico, la memoria se libera de inmediato, evitando el uso de almacenamiento fisico en base de datos o disco.
