# Documentación Funcional del Sistema — PEMS (Playzone Event Management System)

**Autor:** Wilian Lopez

Esta documentación describe el funcionamiento completo del sistema de gestión de eventos, ventas y finanzas: qué hace cada módulo, qué reglas de negocio aplica, y cómo se relacionan entre sí. Cubre Caja, Ventas, Ingresos, Egresos, Gastos Operativos, Reservas Públicas, Eventos Privados y Reportes Financieros.

---

## 1. Visión general

El sistema administra la operación de uno o varios locales ("sedes") de entretenimiento infantil: venta de entradas (reservas públicas), organización de eventos privados (cumpleaños/celebraciones), cobro en caja física y administrativa, y el registro financiero completo de ingresos y egresos.

Cada sede opera de forma independiente en cuanto a caja, ventas y reportes. Un usuario de operación (cajero, recepción, gestor de eventos) trabaja dentro de su propia sede; los roles administrativos (`ADMIN`, `SUPERADMIN`) pueden operar y consultar cualquier sede.

### 1.1 Roles

| Rol | Alcance |
|---|---|
| `SUPERADMIN` / `ADMIN` | Acceso a todas las sedes, aprobación de egresos, cierres forzados de caja, reportes globales. |
| Staff de sede (cajero, recepción, gestor de eventos) | Acceso restringido a su propia sede (caja, ventas, reservas, eventos, ingresos/egresos). Cada acción sobre un recurso valida que ese recurso pertenezca a la sede del usuario. |
| `CLIENTE` | Usuario final. Puede crear sus propias reservas/eventos, consultar su propio historial, ver y descargar el contrato en PDF de sus eventos privados (cargado por el staff, sin capacidad de edición ni firma electrónica), y subir comprobantes de pago. No tiene sede asignada: sus permisos se validan por titularidad (es su propio registro), no por sede. |

### 1.2 Permisos por módulo (autoridades)

El acceso a cada acción está controlado por permisos explícitos (ejemplos): `caja.abrir`, `caja.cerrar`, `caja.movimiento`, `caja.ver_historial`, `ingreso.ver`, `ingreso.crear`, `ingreso.eliminar`, `egreso.ver`, `egreso.crear`, `egreso.eliminar`, `catalogo.editar`, `finanzas.reportes`, `finanzas.ver`, `reserva.ver`, `reserva.crear`, `reserva.cancelar`, `reserva.reprogramar`, `reserva.confirmar_pago`, `reserva.marcar_ingreso`, `reserva.editar`, `evento.ver`, `evento.crear`, `evento.confirmar`, `pos.vender`, `dashboard.ver`. Cada endpoint exige el permiso correspondiente además de, cuando aplica, pertenecer a la sede del recurso solicitado.

---

## 2. Caja

### 2.1 Sesión de caja

Una **sesión de caja** representa el turno de trabajo de un usuario en una sede: se abre al iniciar el turno y se cierra al finalizarlo. Existen dos tipos de sesión:

- **CAJERO** — la abre el personal de venta/recepción para cobrar entradas y ventas de mostrador.
- **ADMINISTRATIVA** — la abre un `ADMIN`/`SUPERADMIN` (o gestor autorizado) para registrar cobros de eventos privados, ingresos manuales y egresos en efectivo.

Reglas:
- Un usuario solo puede tener **una sesión abierta a la vez** (en cualquier sede). Si intenta abrir otra sin cerrar la anterior, el sistema lo rechaza.
- El saldo inicial no puede ser negativo.
- Mientras la sesión está **ABIERTA**, acumula automáticamente `totalIngresos` y `totalEgresos` conforme se registran movimientos.
- El **saldo esperado** se calcula como `saldoInicial + totalIngresos − totalEgresos`.

### 2.2 Cierre de caja

Al cerrar, el usuario indica el **saldo contado** físicamente. El sistema:
1. Calcula la diferencia entre el saldo contado y el saldo esperado.
2. Si la diferencia (en valor absoluto) supera un **umbral configurable** (`CAJA_UMBRAL_DIFERENCIA`), exige que el usuario registre una observación que la justifique; de lo contrario, rechaza el cierre.
3. Permite que un `ADMIN` cierre la caja de otro usuario, siempre con un motivo obligatorio (cierre "ajeno").
4. Existe además un **cierre forzado**, exclusivo de administradores, para casos excepcionales (usuario que no puede cerrar su propia caja), que exige un motivo explícito.
5. Una vez cerrada, la sesión no admite más movimientos, arqueos ni anulaciones.

### 2.3 Movimientos de caja

Cada entrada o salida de dinero físico dentro de una sesión se registra como un **movimiento de caja** (`INGRESO` o `EGRESO`). Los movimientos pueden ser:

- **Automáticos** (`esManual = false`): generados por el sistema al cobrarse una venta, reserva o evento en efectivo.
- **Manuales** (`esManual = true`): registrados directamente por el usuario (por ejemplo, un retiro de efectivo para pago a un proveedor, con una `categoriaRetiro`).

Los movimientos **nunca se eliminan**. Para revertir uno, se genera un **contraasiento**: un nuevo movimiento del mismo tipo y monto, marcado con naturaleza `CONTRAASIENTO` y referenciando al movimiento original (`idMovimientoAnulado`), que resta su efecto de los totales de la sesión. Un contraasiento no puede volver a anularse, y un movimiento no puede anularse dos veces (protegido a nivel de base de datos).

**Enrutamiento automático a caja:** cuando una venta, reserva o evento se cobra en efectivo, el sistema busca automáticamente la sesión de caja abierta del usuario que realiza el cobro y le suma el ingreso. Si el usuario no tiene una sesión abierta (o no es del tipo requerido — administrativa para cobros de eventos/ingresos/egresos manuales), la operación se rechaza con un mensaje claro ("Abre tu caja antes de cobrar"). **Los pagos en medios distintos a efectivo (Yape, transferencia, tarjeta) no generan movimiento de caja física** — se registran únicamente como ingreso financiero (ver §3 y §4).

### 2.4 Arqueos

Un **arqueo** es un conteo de efectivo realizado durante el turno (sin cerrar la caja), útil para detectar diferencias tempranamente. Registra el saldo esperado en ese momento, el saldo contado y la diferencia. No afecta los totales de la sesión, es solo un punto de control auditable.

### 2.5 Resumen de caja

Para cada sesión, el sistema puede generar un resumen completo: saldo inicial, ingresos y egresos totales, saldo esperado, saldo final (si está cerrada), diferencia, y el detalle completo de movimientos y arqueos asociados.

### 2.6 Acceso a la información de caja

Una sesión de caja, sus movimientos y arqueos solo son visibles/operables por: (a) el usuario dueño de la sesión, o (b) un `ADMIN`/`SUPERADMIN`. Esto aplica independientemente de la sede.

---

## 3. Ventas

### 3.1 Venta de mostrador

Cobro presencial de entradas para uno o más niños en el momento, sin reserva previa. El flujo:
1. Valida que el usuario tenga una **caja abierta en la sede indicada** (no basta con tener cualquier caja abierta en otra sede).
2. Resuelve la tarifa vigente según la fecha de visita (día de semana vs. fin de semana/feriado) y la sede.
3. Aplica una promoción si corresponde (validando vigencia y mínimo de personas).
4. Calcula subtotal, descuento y total a partir de la tarifa y promoción resueltas **en el servidor** (el precio nunca se toma de lo que envía el cliente).
5. Registra la venta, una reserva pública por cada niño (con su propio ticket y código QR) y los pagos declarados.
6. Enruta cada pago en efectivo hacia la caja del cajero.
7. Determina automáticamente si la visita es "hoy" y si ya se puede marcar el ingreso, o si queda confirmada para una fecha futura.

### 3.2 Cobro de una reserva pendiente

Cuando un cliente reservó por la web y aún no pagó (o el pago quedó pendiente de validar), el personal puede cobrarla en mostrador: valida el horario de atención de la sede, calcula el vuelto según lo recibido, genera o actualiza la venta asociada, registra los pagos y enruta el efectivo a caja. Según la hora y fecha, la reserva puede quedar `CONFIRMADA` (para otro día) o directamente `COMPLETADA` (ingreso inmediato).

### 3.3 Documentos de venta

Cada venta puede: reenviarse por correo al cliente, marcarse como impresa o descargada (nota de venta en PDF), y consultarse en detalle (líneas, tickets asociados, pagos con su estado de validación).

---

## 4. Ingresos

Los **ingresos** son el registro contable de todo dinero que entra, más allá de si tocó o no la caja física.

### 4.1 Ingresos automáticos
Se generan solos cuando: se cobra una reserva pública, se confirma un pago web, se registra un adelanto o cuota de un evento privado. Quedan vinculados a la reserva/evento/venta que los originó.

### 4.2 Ingresos manuales
Un usuario administrativo puede registrar un ingreso que no proviene de una venta (por ejemplo, un reembolso de proveedor). Requiere un tipo de ingreso activo del catálogo, monto, fecha, medio de pago y descripción. Si el medio es efectivo, se enruta a la caja administrativa del usuario.

### 4.3 Tesorería Web
Vista de conciliación de todos los pagos digitales (Yape, transferencia) originados por reservas online: por definición **nunca afectan una caja física**, ya que no son efectivo. Sirve para que el equipo administrativo concilie lo cobrado por la web contra lo depositado/transferido realmente.

### 4.4 Anulación de ingresos
Un ingreso no se edita ni se borra. Se **anula**, lo que genera un contraasiento (mismo monto, naturaleza `CONTRAASIENTO`, referenciando el ingreso original) con un motivo obligatorio. Un ingreso ya anulado no puede volver a anularse (protegido también a nivel de base de datos ante intentos simultáneos).

### 4.5 Reportes
Los reportes financieros (mensual, diario, por rango) **excluyen automáticamente** los ingresos anulados y sus contraasientos del total — de modo que el ingreso anulado no infla ni descuadra el reporte, pero **sigue siendo visible en los listados** para trazabilidad y auditoría.

---

## 5. Egresos

Los **egresos** representan salidas de dinero: pago a proveedores, servicios, personal, gastos eventuales, etc., clasificados por un catálogo de tipos de egreso (con categoría: fijo recurrente, variable recurrente, eventual).

### 5.1 Registro y aprobación por monto
Al registrar un egreso, el sistema compara el monto contra un **umbral configurable** (`EGRESO_UMBRAL_APROBACION`):
- Si el monto es **menor** al umbral, el egreso queda `APROBADO` de inmediato y se enruta a caja (si es en efectivo).
- Si el monto es **igual o mayor**, el egreso queda `PENDIENTE_APROBACION` y **no afecta caja todavía** — solo queda contabilizado tras ser aprobado.

### 5.2 Aprobar / Rechazar
Solo un `ADMIN`/`SUPERADMIN` puede aprobar o rechazar un egreso pendiente, y **nunca puede ser la misma persona que lo registró** (separación de funciones/doble control). Al aprobar, recién en ese momento se enruta el efectivo a la caja administrativa del aprobador. Al rechazar, se exige un motivo y el egreso queda marcado como `RECHAZADO` sin afectar caja. Esta operación usa bloqueo a nivel de fila para evitar que dos aprobaciones/rechazos simultáneos sobre el mismo egreso produzcan un doble descuento de caja.

### 5.3 Anulación de egresos
Solo un egreso ya `APROBADO` puede anularse. La anulación genera un contraasiento (igual patrón que ingresos), con motivo obligatorio, y no se puede anular dos veces.

### 5.4 Reportes
Igual que los ingresos: los totales de reportes excluyen egresos anulados/contraasientos y pendientes/rechazados de los cálculos de utilidad, pero se mantienen visibles en los listados.

---

## 6. Gastos Operativos

Gastos del día a día de una sede (limpieza, insumos, mantenimiento menor) que no pasan por el flujo de aprobación de egresos, pero comparten el mismo principio de trazabilidad:

- Se registran con fecha, descripción, monto y comprobante/referencia opcional.
- **No se editan ni se eliminan físicamente.** Se anulan con un contraasiento (mismo patrón `NORMAL`/`CONTRAASIENTO` que ingresos y egresos), con motivo obligatorio.
- Los reportes y sumatorias por sede/fecha/rango excluyen automáticamente los gastos anulados y sus contraasientos.

---

## 7. Gastos de Evento

Gastos específicos asociados a un evento privado puntual (por ejemplo, animador contratado, decoración especial). Se listan y consultan por evento y por sede, y alimentan el cálculo de utilidad bruta de cada evento (ver §10.4).

---

## 8. Reservas Públicas

Ciclo de vida de una entrada reservada por un cliente (presencial o vía web):

1. **Creación (`PENDIENTE`)** — el cliente elige fecha, se valida disponibilidad (aforo, feriados, bloqueos de calendario, antelación mínima/máxima) y se calcula el precio según la tarifa vigente. Si el pago es digital, queda a la espera de validación.
2. **Confirmación de pago (`CONFIRMADA`)** — el administrador valida el comprobante (Yape/transferencia) y confirma la reserva; se genera el ticket con código QR.
3. **Rechazo de pago** — si el comprobante no es válido, se rechaza indicando motivo y se solicita al cliente subir uno nuevo; la reserva permanece `PENDIENTE`.
4. **Reprogramación** — el cliente o el staff puede mover la reserva a otra fecha (dentro de un límite de reprogramaciones configurado); si la nueva tarifa es mayor a lo ya pagado, se genera un cobro adicional pendiente.
5. **Ingreso / control de acceso** — el día de la visita, el personal de acceso busca el ticket por número o código QR y marca el ingreso, validando que la fecha sea la correcta, que no haya ingresado ya, que no esté cancelada, que el pago no esté pendiente y que tenga una venta asociada.
6. **Cancelación (`CANCELADA`)** — con motivo obligatorio.
7. **Estados terminales:** `COMPLETADA` (ya ingresó) y `CANCELADA` no permiten más acciones.

**Eliminación administrativa:** solo se permite eliminar físicamente una reserva que **no tiene ninguna venta asociada** (es decir, que nunca llegó a cobrarse). Si ya generó una venta y pagos, debe cancelarse por el flujo normal, preservando el registro financiero.

**Reglas de aforo:** cada reserva activa (`CONFIRMADA`/`COMPLETADA`) ocupa un cupo del aforo máximo configurado por sede y fecha; el sistema rechaza nuevas reservas si el aforo ya está cubierto.

---

## 9. Eventos Privados (cumpleaños y celebraciones)

Ciclo de vida de una celebración privada contratada por un cliente:

1. **Solicitud (`SOLICITADA`)** — el cliente (o el staff en su nombre) indica fecha, turno, tipo de evento, aforo declarado, paquete/extras deseados o una cotización personalizada. Se valida antelación mínima/máxima (salvo administradores), feriados, bloqueos de calendario y que no exista ya un evento en ese turno/fecha, ni una reserva pública que ocupe la fecha.
2. **Confirmación (`CONFIRMADA`)** — se fija el precio de contrato final y la modalidad de pago:
   - **AL_CONTADO** — un adelanto opcional al confirmar.
   - **CUOTAS** — se genera un cronograma de cuotas (mínimo 2), cada una con su monto y fecha de vencimiento; el adelanto (si existe) se registra como la primera cuota ya pagada.
   Al confirmar se crea también el checklist operativo base del evento.
3. **Pagos de cuotas / saldo** — cada cuota se paga con uno o varios medios de pago; el sistema valida que el total pagado coincida exactamente con el monto de la cuota. También existe un registro de "saldo libre" (sin cronograma de cuotas) para abonos parciales, validando que no exceda el saldo pendiente del contrato.
4. **Checklist operativo** — lista de tareas asociadas al evento (montaje, verificación de paquete, etc.) que el staff va completando/descompletando; cada tarea pertenece a un único evento y no puede operarse desde otro evento.
5. **Completar (`COMPLETADA`)** — solo si el evento está `CONFIRMADA` y la fecha del evento ya pasó.
6. **Cancelación (`CANCELADA`)** — con motivo obligatorio, solo si el evento aún puede cancelarse (no está ya completado/cancelado).

Cada pago de adelanto, cuota o saldo genera automáticamente su ingreso financiero y, si es en efectivo, su movimiento de caja administrativa.

---

## 10. Reportes y Dashboard

### 10.1 Dashboard operativo
Resumen del día por sede: reservas de hoy/ayer, ingresos del día, reservas confirmadas y pendientes de pago, aforo disponible, eventos de la semana, solicitudes de evento sin responder, eventos con saldo pendiente, si la caja está abierta, y comprobantes (Yape) por validar.

### 10.2 Dashboard financiero
Totales mensuales de ingresos y egresos desglosados por origen (reservas, adelantos de eventos, ingreso manual) y por categoría de egreso (fijo, variable, eventual), utilidad neta, comparación contra el mes anterior, y serie diaria para gráficos.

### 10.3 Resumen financiero (mensual / diario / por rango)
Ingresos totales (reservas + adelantos de eventos + otros), egresos totales (generales + operativos + de eventos), utilidad neta, y desglose de egresos por tipo/categoría. Todos estos cálculos **excluyen anulados y pendientes/rechazados**, mostrando solo lo efectivamente vigente.

### 10.4 Resumen financiero de un evento
Ingreso de contrato, monto adelantado, total de gastos adicionales del evento, y utilidad bruta resultante (ingreso de contrato menos gastos).

### 10.5 Métricas de reservas
Cantidad de reservas confirmadas/canceladas/completadas en el mes, ingreso total, ticket promedio, e ingreso desglosado por medio de pago (efectivo vs. Yape).

---

## 11. Principios transversales del sistema

- **Nunca se elimina un movimiento financiero ya confirmado.** Ingresos, egresos, gastos operativos y movimientos de caja siempre se revierten mediante un **contraasiento** (naturaleza `CONTRAASIENTO`) que referencia al registro original. Esto garantiza trazabilidad completa: todo el historial permanece consultable, incluso lo anulado.
- **Un registro anulado no puede anularse dos veces.** Esto está protegido tanto en la lógica de aplicación como con una restricción única a nivel de base de datos, de modo que ni siquiera una condición de carrera (dos anulaciones simultáneas) puede duplicar el efecto.
- **Todo movimiento de dinero requiere un responsable identificado.** Cada ingreso, egreso, movimiento de caja, aprobación, rechazo y anulación queda asociado al usuario que lo ejecutó y a la fecha/hora exacta.
- **Cada operación queda scopeada a una sede**, salvo para roles administrativos globales: un usuario de sede jamás puede leer ni operar recursos (ventas, reservas, eventos, caja, ingresos, egresos) de otra sede.
- **El precio de una venta siempre se calcula en el servidor** a partir de la tarifa vigente y la promoción aplicable — nunca se acepta un precio o descuento enviado directamente por el cliente.
- **El efectivo cobrado siempre pasa por una sesión de caja abierta.** No existe forma de registrar un cobro en efectivo sin una caja abierta del tipo correspondiente (cajero para ventas de mostrador, administrativa para ingresos/egresos manuales y cobros de eventos).
