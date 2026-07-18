
INSERT INTO rol (codigo, nombre, descripcion, es_sistema, orden) VALUES
    ('SUPERADMIN',    'Super administrador',  'Acceso total al sistema (no eliminable)',    TRUE, 1),
    ('ADMIN',         'Administrador',        'Gestión completa del negocio',                TRUE, 2),
    ('CAJERO',        'Cajero',               'Atención en mostrador, POS y caja',           TRUE, 3),
    ('CLIENTE',       'Cliente',              'Cliente registrado del sitio web',            TRUE, 4);


INSERT INTO permiso (codigo, modulo, nombre, descripcion, orden) VALUES
    -- Dashboard y calendario
    ('dashboard.ver',           'dashboard',  'Ver dashboard',                'Acceso al dashboard principal',           10),
    ('calendario.ver',          'calendario', 'Ver calendario',               'Ver agenda y disponibilidad',             20),
    ('calendario.configurar',   'calendario', 'Configurar calendario',        'Editar parámetros del calendario',        21),
    ('calendario.bloquear',     'calendario', 'Bloquear fechas',              'Crear bloqueos de calendario',            22),
    ('calendario.feriado',      'calendario', 'Gestionar feriados',           'Registrar feriados',                       23),

    -- Reservas públicas
    ('reserva.ver',             'reserva',    'Ver reservas',                 'Listar reservas públicas',                30),
    ('reserva.crear',           'reserva',    'Crear reserva',                'Crear nueva reserva pública',             31),
    ('reserva.editar',          'reserva',    'Editar reserva',               'Editar datos de reserva',                  32),
    ('reserva.cancelar',        'reserva',    'Cancelar reserva',             'Cancelar reserva',                         33),
    ('reserva.reprogramar',     'reserva',    'Reprogramar reserva',          'Cambiar fecha de reserva',                34),
    ('reserva.confirmar_pago',  'reserva',    'Confirmar pago de reserva',    'Validar pago manual (Yape)',              35),
    ('reserva.marcar_ingreso',  'reserva',    'Marcar ingreso',               'Registrar entrada con QR',                 36),

    -- Eventos privados
    ('evento.ver',              'evento',     'Ver eventos',                  'Listar eventos privados',                  40),
    ('evento.crear',            'evento',     'Crear evento',                 'Registrar nuevo evento',                   41),
    ('evento.editar',           'evento',     'Editar evento',                'Editar datos de evento',                   42),
    ('evento.cancelar',         'evento',     'Cancelar evento',              'Cancelar evento',                          43),
    ('evento.confirmar',        'evento',     'Confirmar evento',             'Aprobar solicitud de evento',              44),
    ('evento.marcar_ingreso',   'evento',     'Check-in de invitados',        'Registrar ingreso al evento',              45),
    ('evento.contrato',         'evento',     'Gestionar contrato',           'CRUD contratos de evento',                 46),

    -- POS y caja
    ('pos.vender',              'pos',        'Venta presencial',             'Generar tickets en mostrador',             50),
    ('caja.abrir',              'caja',       'Abrir caja',                   'Abrir caja del turno',                     51),
    ('caja.cerrar',             'caja',       'Cerrar caja',                  'Cerrar caja del turno',                    52),
    ('caja.movimiento',         'caja',       'Registrar movimiento',         'Movimientos manuales en caja',             53),
    ('caja.ver_historial',      'caja',       'Ver historial',                'Historial de cajas cerradas',              54),

    -- Finanzas
    ('finanzas.ver',            'finanzas',   'Ver finanzas',                 'Resumen financiero',                       60),
    ('ingreso.ver',             'finanzas',   'Ver ingresos',                 'Listar ingresos',                          61),
    ('ingreso.crear',           'finanzas',   'Registrar ingreso manual',     'Crear ingreso manual',                     62),
    ('ingreso.eliminar',        'finanzas',   'Eliminar ingreso',             'Borrar ingreso (solo manuales)',           63),
    ('egreso.ver',              'finanzas',   'Ver egresos',                  'Listar egresos',                           64),
    ('egreso.crear',            'finanzas',   'Registrar egreso',             'Crear egreso',                             65),
    ('egreso.editar',           'finanzas',   'Editar egreso',                'Editar egreso',                            66),
    ('egreso.eliminar',         'finanzas',   'Eliminar egreso',              'Borrar egreso',                            67),
    ('finanzas.reportes',       'finanzas',   'Ver reportes',                 'Reportes financieros consolidados',        68),

    -- Catálogo comercial
    ('paquete.ver',             'catalogo',   'Ver paquetes',                 'Listar paquetes',                          70),
    ('paquete.gestionar',       'catalogo',   'Gestionar paquetes',           'CRUD paquetes',                            71),
    ('promocion.ver',           'catalogo',   'Ver promociones',              'Listar promociones',                       72),
    ('promocion.gestionar',     'catalogo',   'Gestionar promociones',        'CRUD promociones',                         73),
    ('tarifa.ver',              'catalogo',   'Ver tarifas',                  'Ver precios vigentes',                     74),
    ('tarifa.gestionar',        'catalogo',   'Gestionar tarifas',            'CRUD tarifas',                             75),

    -- Sitio web / CMS
    ('sitio.zona',              'sitio',      'Gestionar zonas',              'CRUD zonas de juego',                      80),
    ('sitio.actividad',         'sitio',      'Gestionar actividades',        'CRUD actividades',                         81),
    ('sitio.novedad',           'sitio',      'Gestionar novedades',          'CRUD novedades',                           82),
    ('sitio.banner',            'sitio',      'Gestionar banners',            'CRUD banners',                             83),
    ('sitio.galeria',           'sitio',      'Gestionar galería',            'CRUD imágenes de galería',                 84),
    ('sitio.resena',            'sitio',      'Moderar reseñas',              'Aprobar/rechazar reseñas',                 85),
    ('sitio.faq',               'sitio',      'Gestionar FAQ',                'CRUD preguntas frecuentes',                86),
    ('sitio.legal',             'sitio',      'Editar contenido legal',       'Editar términos, privacidad, etc.',        87),
    ('sitio.contenido',         'sitio',      'Editar contenido web',         'Textos dinámicos del sitio',               88),
    ('sitio.mensaje',           'sitio',      'Ver mensajes del sitio',       'Mensajes de contacto',                     89),
    ('sitio.publica',           'sitio',      'Configuración pública',        'Editar datos del negocio en el sitio',     90),

    -- Clientes y marketing
    ('cliente.ver',             'cliente',    'Ver clientes',                 'Listar clientes',                          100),
    ('cliente.crear',           'cliente',    'Crear cliente',                'Registrar cliente manualmente',            101),
    ('cliente.editar',          'cliente',    'Editar cliente',               'Editar datos de cliente',                  102),
    ('marketing.campana',       'marketing',  'Gestionar campañas',           'CRUD campañas de email',                   103),
    ('marketing.plantilla',     'marketing',  'Gestionar plantillas',         'CRUD plantillas de email',                 104),
    ('marketing.enviar',        'marketing',  'Enviar campañas',              'Disparar envío de campañas',               105),

    -- Facturación
    ('comprobante.ver',         'facturacion','Ver comprobantes',             'Listar comprobantes',                      110),
    ('comprobante.emitir',      'facturacion','Emitir comprobante',           'Emitir boleta/factura',                    111),
    ('comprobante.anular',      'facturacion','Anular comprobante',           'Emitir nota de crédito',                   112),

    -- Sistema
    ('usuarios.gestionar',      'usuario',    'Gestionar usuarios',           'CRUD cuentas y roles',                     121),
    ('rol.gestionar',           'usuario',    'Gestionar roles',              'Asignar roles y permisos',                 122),
    ('configuracion.editar',    'sistema',    'Editar configuración',         'Configuración global/sede',                123),
    ('auditoria.ver',           'sistema',    'Ver auditoría',                'Log de auditoría',                         124),
    ('catalogo.editar',         'sistema',    'Editar catálogos',             'CRUD catálogos editables',                 125);



INSERT INTO rol_permiso (rol_codigo, permiso_codigo)
SELECT 'SUPERADMIN', codigo FROM permiso;

INSERT INTO rol_permiso (rol_codigo, permiso_codigo)
SELECT 'ADMIN', codigo FROM permiso WHERE codigo NOT IN ('rol.gestionar');

INSERT INTO rol_permiso (rol_codigo, permiso_codigo) VALUES
    ('CAJERO','dashboard.ver'),
    ('CAJERO','calendario.ver'),
    ('CAJERO','reserva.ver'),
    ('CAJERO','reserva.crear'),
    ('CAJERO','reserva.editar'),
    ('CAJERO','reserva.confirmar_pago'),
    ('CAJERO','reserva.marcar_ingreso'),
    ('CAJERO','evento.ver'),
    ('CAJERO','evento.marcar_ingreso'),
    ('CAJERO','pos.vender'),
    ('CAJERO','caja.abrir'),
    ('CAJERO','caja.cerrar'),
    ('CAJERO','caja.movimiento'),
    ('CAJERO','paquete.ver'),
    ('CAJERO','promocion.ver'),
    ('CAJERO','tarifa.ver'),
    ('CAJERO','cliente.ver'),
    ('CAJERO','cliente.crear'),
    ('CAJERO','cliente.editar'),
    ('CAJERO','comprobante.ver'),
    ('CAJERO','comprobante.emitir');


INSERT INTO estado_reserva (codigo, nombre, descripcion, es_terminal, orden) VALUES
    ('PENDIENTE',    'Pendiente',     'Reserva creada, pago pendiente de validación',   FALSE, 1),
    ('CONFIRMADA',   'Confirmada',    'Pago validado, ticket activo',                    FALSE, 2),
    ('REPROGRAMADA', 'Reprogramada',  'Entrada movida a otra fecha',                     FALSE, 3),
    ('COMPLETADA',   'Completada',    'Visita realizada',                                 TRUE,  4),
    ('CANCELADA',    'Cancelada',     'Reserva cancelada',                                TRUE,  5);

INSERT INTO estado_evento (codigo, nombre, descripcion, es_terminal, orden) VALUES
    ('SOLICITADA',   'Solicitada',    'Solicitud recibida, pendiente de contacto',       FALSE, 1),
    ('CONFIRMADA',   'Confirmada',    'Contrato firmado, evento agendado',                FALSE, 2),
    ('EN_CURSO',     'En curso',      'Evento en ejecución',                              FALSE, 3),
    ('COMPLETADA',   'Completada',    'Evento finalizado',                                TRUE,  4),
    ('CANCELADA',    'Cancelada',     'Evento cancelado',                                 TRUE,  5);

INSERT INTO estado_contrato (codigo, nombre, descripcion, es_terminal, orden) VALUES
    ('BORRADOR',         'Borrador',          'En redacción, sin enviar',                FALSE, 1),
    ('ENVIADO',          'Enviado',           'Enviado al cliente para revisión',         FALSE, 2),
    ('PENDIENTE_FIRMA',  'Pendiente firma',   'Pendiente de firma',                        FALSE, 3),
    ('FIRMADO',          'Firmado',           'Firmado por ambas partes',                  TRUE,  4),
    ('VENCIDO',          'Vencido',           'Expirado sin firma',                        TRUE,  5),
    ('CANCELADO',        'Cancelado',         'Cancelado antes de la firma',               TRUE,  6),
    ('ARCHIVADO',        'Archivado',         'Archivado tras evento completado',          TRUE,  7);

INSERT INTO estado_comprobante (codigo, nombre, descripcion, es_terminal, orden) VALUES
    ('PENDIENTE',    'Pendiente',     'Pendiente de envío a SUNAT',                       FALSE, 1),
    ('ENVIADO',      'Enviado',       'Enviado a SUNAT, esperando respuesta',             FALSE, 2),
    ('EMITIDO',      'Emitido',       'Validado y aceptado por SUNAT',                    TRUE,  3),
    ('RECHAZADO',    'Rechazado',     'Rechazado por SUNAT, requiere corrección',         FALSE, 4),
    ('ANULADO',      'Anulado',       'Anulado mediante nota de crédito',                 TRUE,  5);

INSERT INTO estado_caja (codigo, nombre, descripcion, es_terminal, orden) VALUES
    ('ABIERTA',      'Abierta',       'Caja activa, acepta movimientos',                  FALSE, 1),
    ('CERRADA',      'Cerrada',       'Caja cerrada, solo lectura',                       TRUE,  2);


INSERT INTO medio_pago (codigo, nombre, descripcion, es_efectivo, es_sistema, orden) VALUES
    ('EFECTIVO',      'Efectivo',           'Pago físico en caja',                  TRUE,  TRUE, 1),
    ('YAPE',          'Yape',               'Pago vía Yape',                         FALSE, TRUE, 2),
    ('TARJETA',       'Tarjeta',            'Pago con tarjeta débito/crédito',      FALSE, TRUE, 3),
    ('TRANSFERENCIA', 'Transferencia',      'Transferencia bancaria',                FALSE, TRUE, 4),
    ('PLIN',          'Plin',               'Pago vía Plin',                         FALSE, FALSE, 5);



INSERT INTO turno (codigo, nombre, hora_inicio, hora_fin, orden) VALUES
    ('T1', 'Turno mañana', '10:00', '14:00', 1),
    ('T2', 'Turno tarde',  '16:00', '20:00', 2);


INSERT INTO tipo_dia (codigo, nombre, descripcion, orden) VALUES
    ('SEMANA',             'Entre semana',             'Lunes a viernes (tarifa A)',         1),
    ('FIN_SEMANA_FERIADO', 'Fin de semana / feriado',  'Sábado, domingo y feriados',          2);



INSERT INTO canal_reserva (codigo, nombre, descripcion, orden) VALUES
    ('WEB',        'Sitio web',     'Reserva online desde el sitio público',   1),
    ('MOSTRADOR',  'Mostrador',     'Registrada en mostrador (POS)',           2);



INSERT INTO tipo_feriado (codigo, nombre, descripcion, orden) VALUES
    ('NACIONAL',  'Nacional',  'Feriado oficial nacional del Perú',  1),
    ('REGIONAL',  'Regional',  'Feriado regional o local',           2);


INSERT INTO tipo_promocion (codigo, nombre, descripcion, es_sistema, orden) VALUES
    ('DESCUENTO_PORCENTAJE', 'Descuento porcentual',  'Descuento % sobre el precio base',           TRUE,  1),
    ('DESCUENTO_MONTO_FIJO', 'Descuento monto fijo',  'Descuento de monto fijo',                     TRUE,  2),
    ('NX1',                  'NxM',                   'N entradas al precio de M (ej. 3x2)',         TRUE,  3),
    ('ENTRADA_GRATUITA',     'Entrada gratuita',      'Entrada sin costo (fidelización)',            TRUE,  4),
    ('PAQUETE_GRUPAL',       'Paquete grupal',        'Precio especial para grupos',                  FALSE, 5);



INSERT INTO tipo_comprobante (codigo, nombre, descripcion, es_electronico, requiere_ruc, orden) VALUES
    ('BOLETA',        'Boleta de venta',     'Boleta electrónica',                  TRUE,  FALSE, 1),
    ('FACTURA',       'Factura',             'Factura electrónica (requiere RUC)',  TRUE,  TRUE,  2),
    ('NOTA_CREDITO',  'Nota de crédito',     'Anulación de comprobante',            TRUE,  FALSE, 3),
    ('NOTA_VENTA',    'Nota de venta',       'Documento interno (no SUNAT)',         FALSE, FALSE, 4);


INSERT INTO tipo_documento (codigo, nombre, longitud, orden) VALUES
    ('DNI',         'DNI',          8,    1),
    ('RUC',         'RUC',          11,   2),
    ('CE',          'Carnet de extranjería', 9,  3),
    ('PASAPORTE',   'Pasaporte',    NULL, 4),
    ('SIN_DOC',     'Sin documento', NULL, 5);



INSERT INTO tipo_ingreso (codigo, nombre, descripcion, es_sistema, orden) VALUES
    ('RESERVA_PUBLICA', 'Reserva pública',          'Ingreso por entradas al parque',           TRUE,  1),
    ('ADELANTO_EVENTO', 'Adelanto evento privado',  'Adelanto por contrato de evento',          TRUE,  2),
    ('SALDO_EVENTO',    'Saldo evento privado',     'Pago restante del evento',                  TRUE,  3),
    ('INGRESO_MANUAL',  'Ingreso manual',           'Registrado manualmente',                    FALSE, 4),
    ('OTRO',            'Otro ingreso',             'Categoría libre',                           FALSE, 5);



INSERT INTO tipo_egreso (codigo, nombre, descripcion, categoria, orden) VALUES
    ('ELECTRICIDAD',   'Electricidad',   'Servicio eléctrico',           'RECURRENTE_VARIABLE', 1),
    ('AGUA',           'Agua',           'Servicio de agua',              'RECURRENTE_VARIABLE', 2),
    ('INTERNET',       'Internet',       'Servicio de internet',          'RECURRENTE_FIJO',     3),
    ('ALQUILER',       'Alquiler local', 'Pago del alquiler',             'RECURRENTE_FIJO',     4),
    ('SUELDOS',        'Sueldos',        'Pago de personal',              'RECURRENTE_VARIABLE', 5),
    ('LIMPIEZA',       'Limpieza',       'Insumos y servicios',           'RECURRENTE_VARIABLE', 6),
    ('REPARACION',     'Reparaciones',   'Mantenimiento',                 'EVENTUAL',            7),
    ('EQUIPAMIENTO',   'Equipamiento',   'Compra de equipos',             'EVENTUAL',            8);


INSERT INTO tipo_evento (codigo, nombre, descripcion, icono, orden) VALUES
    ('CUMPLEANOS',    'Cumpleaños',         'Fiesta de cumpleaños infantil',    'cake',          1),
    ('BABY_SHOWER',   'Baby shower',        'Celebración de bebé en camino',    'baby',          2),
    ('PRIMERA_HORA',  'Primera hora',       'Bienvenida del bebé',              'gift',           3),
    ('GRADUACION',    'Graduación',         'Graduación escolar/inicial',        'graduation-cap', 4),
    ('FIESTA_PRIVADA','Fiesta privada',     'Evento privado general',            'party-popper',  5),
    ('CORPORATIVO',   'Evento corporativo', 'Evento de empresa',                 'briefcase',     6),
    ('OTRO',          'Otro',               'Tipo no clasificado',               'sparkles',      99);


INSERT INTO tipo_notificacion (codigo, modulo, nombre, descripcion, destinatario_default, canales_default, prioridad, es_sistema, orden) VALUES
    -- Reservas (cliente)
    ('RESERVA_CONFIRMADA',       'reserva', 'Reserva confirmada',          'Confirmación al cliente',                'CLIENTE', ARRAY['IN_APP','EMAIL'],          'NORMAL',  TRUE, 10),
    ('RESERVA_RECORDATORIO',     'reserva', 'Recordatorio de visita',      'Recordatorio 24h antes',                 'CLIENTE', ARRAY['EMAIL','WHATSAPP'],       'NORMAL',  TRUE, 11),
    ('RESERVA_CANCELADA',        'reserva', 'Reserva cancelada',           'Aviso al cliente',                       'CLIENTE', ARRAY['IN_APP','EMAIL'],          'NORMAL',  TRUE, 12),
    -- Reservas (admin)
    ('RESERVA_YAPE_PENDIENTE',   'reserva', 'Yape por validar',            'Validación de pago Yape',                'ADMIN',   ARRAY['IN_APP'],                  'ALTA',    TRUE, 13),
    -- Eventos (admin)
    ('EVENTO_SOLICITUD',         'evento',  'Nueva solicitud de evento',   'Llegó una solicitud nueva',              'ADMIN',   ARRAY['IN_APP','EMAIL'],          'ALTA',    TRUE, 20),
    ('EVENTO_CONTRATO_PENDIENTE','evento',  'Contrato pendiente',          'Evento cercano sin contrato firmado',    'ADMIN',   ARRAY['IN_APP','EMAIL'],          'CRITICA', TRUE, 21),
    ('EVENTO_SALDO_PENDIENTE',   'evento',  'Saldo pendiente',             'Evento próximo con saldo por cobrar',    'ADMIN',   ARRAY['IN_APP'],                  'ALTA',    TRUE, 22),
    -- Eventos (cliente)
    ('EVENTO_CONFIRMADO',        'evento',  'Evento confirmado',           'Tu evento fue confirmado',               'CLIENTE', ARRAY['IN_APP','EMAIL'],          'NORMAL',  TRUE, 23),
    ('EVENTO_RECORDATORIO',      'evento',  'Recordatorio de evento',      'Tu evento es en X días',                 'CLIENTE', ARRAY['IN_APP','EMAIL','WHATSAPP'],'NORMAL', TRUE, 24),
    -- Caja
    ('CAJA_SIN_CERRAR',          'caja',    'Caja sin cerrar',             'Caja del día sigue abierta',             'ADMIN',   ARRAY['IN_APP'],                  'ALTA',    TRUE, 30),
    ('CAJA_SIN_ABRIR',           'caja',    'Caja sin abrir',              'Caja no se ha abierto al iniciar el día','CAJERO',  ARRAY['IN_APP'],                  'NORMAL',  TRUE, 31),
    -- Aforo y operación
    ('AFORO_LIMITE',             'reserva', 'Aforo cercano al límite',     'Aforo > 90% para una fecha próxima',     'ADMIN',   ARRAY['IN_APP'],                  'NORMAL',  TRUE, 40),
    -- CMS
    ('RESENA_PENDIENTE',         'sitio',   'Reseña pendiente',            'Reseña nueva por moderar',               'ADMIN',   ARRAY['IN_APP'],                  'BAJA',    TRUE, 50),
    ('MENSAJE_NUEVO',            'sitio',   'Mensaje nuevo',               'Mensaje desde el sitio público',         'ADMIN',   ARRAY['IN_APP','EMAIL'],          'NORMAL',  TRUE, 51),
    -- Cliente
    ('BIENVENIDA',               'cliente', 'Bienvenida',                  'Cuenta creada',                           'CLIENTE', ARRAY['EMAIL'],                   'NORMAL',  TRUE, 60),
    ('CUMPLEANOS_NINO',          'cliente', 'Cumpleaños del niño',         'Saludo automático en la fecha',          'CLIENTE', ARRAY['EMAIL'],                   'NORMAL',  TRUE, 61),
    -- Sistema
    ('LOGIN_IP_NUEVA',           'sistema', 'Login desde IP nueva',        'Acceso desde dispositivo no reconocido', 'ADMIN',   ARRAY['EMAIL'],                   'CRITICA', TRUE, 70);



INSERT INTO seccion_web (codigo, nombre, descripcion, orden) VALUES
    ('HOME',        'Página principal',     'Contenido principal del home',  1),
    ('HEADER',      'Cabecera',             'Navbar público',                 2),
    ('FOOTER',      'Pie de página',        'Footer del sitio',               3),
    ('CONTACTO',    'Contacto',             'Información de contacto',        4),
    ('NOSOTROS',    'Nosotros',             'Contenido institucional',        5),
    ('PROMOCIONES', 'Promociones',          'Promociones públicas',           6),
    ('GALERIA',     'Galería',              'Galería pública',                7),
    ('FAQ',         'Preguntas frecuentes', 'FAQs del sitio',                 8),
    ('LEGAL',       'Contenido legal',      'Políticas y términos',           9);



INSERT INTO tipo_contenido (codigo, nombre, descripcion, orden) VALUES
    ('TEXTO',    'Texto simple',         'Texto plano',                  1),
    ('HTML',     'HTML',                 'Contenido HTML',                2),
    ('URL',      'Enlace URL',           'Enlace externo',                3),
    ('JSON',     'JSON',                 'Estructura JSON',               4),
    ('EMAIL',    'Correo electrónico',   'Dirección de correo',           5),
    ('TELEFONO', 'Teléfono',             'Número telefónico',             6),
    ('COLOR',    'Color',                'Color hexadecimal',             7),
    ('IMAGEN',   'Imagen',               'Path en Storage',               8),
    ('BOOLEANO', 'Booleano',             'Verdadero o falso',             9);


INSERT INTO tipo_email (codigo, nombre, descripcion, es_sistema, orden) VALUES
    ('TRANSACCIONAL', 'Transaccional',   'Emails operativos (confirmaciones, recordatorios)',  TRUE,  1),
    ('MARKETING',     'Marketing',       'Campañas promocionales',                              FALSE, 2),
    ('SISTEMA',       'Sistema',         'Notificaciones internas del sistema',                 TRUE,  3);



INSERT INTO segmento_cliente (codigo, nombre, descripcion, es_sistema, orden) VALUES
    ('NUEVO',        'Nuevo',         'Cliente recién registrado',                TRUE,  1),
    ('FRECUENTE',    'Frecuente',     '3+ visitas en los últimos 3 meses',         TRUE,  2),
    ('VIP',          'VIP',           'Cliente VIP con beneficios especiales',     TRUE,  3),
    ('CORPORATIVO',  'Corporativo',   'Empresa con RUC',                            TRUE,  4),
    ('INACTIVO',     'Inactivo',      'Sin actividad en 6+ meses',                 TRUE,  5);



INSERT INTO sede (nombre, direccion, ciudad, departamento, telefono, correo)
VALUES (
    'Kiki y Lala - Chiclayo',
    'Por definir',
    'Chiclayo',
    'Lambayeque',
    NULL,
    NULL
);


INSERT INTO configuracion_sede (sede_id)
SELECT id FROM sede WHERE deleted_at IS NULL LIMIT 1;


INSERT INTO configuracion_global (clave, valor, tipo_dato, descripcion, es_sistema, es_secreto) VALUES
    ('ZONA_HORARIA',                  'America/Lima', 'TEXTO',   'Zona horaria del sistema',                          TRUE,  FALSE),
    ('INTENTOS_LOGIN_ANTES_BLOQUEO',  '5',            'NUMERO',  'Intentos fallidos antes del bloqueo',                TRUE,  FALSE),
    ('DURACION_BLOQUEO_LOGIN_MIN',    '15',           'NUMERO',  'Duración del bloqueo en minutos',                   TRUE,  FALSE),
    ('EXPIRACION_SESION_ADMIN_MIN',   '120',          'NUMERO',  'Minutos de inactividad para expirar sesión admin',  TRUE,  FALSE),
    ('EXPIRACION_SESION_CLIENTE_MIN', '45',           'NUMERO',  'Minutos de inactividad para expirar sesión cliente',TRUE,  FALSE),
    ('SUNAT_PROVEEDOR',               'NUBEFACT',     'TEXTO',   'Proveedor de facturación electrónica',              TRUE,  FALSE),
    ('SUNAT_API_URL',                 '',             'TEXTO',   'Endpoint del proveedor SUNAT',                      TRUE,  TRUE),
    ('SUNAT_API_TOKEN',               '',             'TEXTO',   'Token de autenticación SUNAT',                      TRUE,  TRUE),
    ('DECOLECTA_API_URL',             'https://api.decolecta.com/v1', 'TEXTO', 'Endpoint API consulta DNI',           TRUE,  FALSE),
    ('DECOLECTA_API_TOKEN',           '',             'TEXTO',   'Token API Decolecta',                                TRUE,  TRUE),
    ('CACHE_DNI_DIAS',                '180',          'NUMERO',  'Días de validez del cache de DNI',                   TRUE,  FALSE),
    ('WHATSAPP_NUMERO',               '',             'TEXTO',   'Número WhatsApp Business del negocio',              FALSE, FALSE),
    ('STORAGE_BUCKET_PUBLICO',        'kiki-publico', 'TEXTO',   'Bucket Supabase Storage público',                    TRUE,  FALSE),
    ('STORAGE_BUCKET_PRIVADO',        'kiki-privado', 'TEXTO',   'Bucket Supabase Storage privado',                    TRUE,  FALSE),
    ('STORAGE_BUCKET_TEMPORAL',       'kiki-temporal','TEXTO',   'Bucket temporal con TTL',                            TRUE,  FALSE);


INSERT INTO configuracion_publica (
    nombre_negocio, slogan,
    color_primario, color_secundario,
    copyright_texto
) VALUES (
    'Kiki y Lala',
    'Diversión sin límites para los más pequeños',
    '#00AEEF',
    '#F64B8A',
    '© Kiki y Lala. Todos los derechos reservados.'
);