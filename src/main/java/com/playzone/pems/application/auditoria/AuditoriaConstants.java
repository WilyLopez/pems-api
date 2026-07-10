package com.playzone.pems.application.auditoria;

public final class AuditoriaConstants {

    private AuditoriaConstants() {}

    // ── Módulos ──────────────────────────────────────────────────────────────
    public static final String MOD_ACCESOS       = "ACCESOS";
    public static final String MOD_USUARIOS      = "USUARIOS";
    public static final String MOD_CONTRATOS     = "CONTRATOS";
    public static final String MOD_VENTAS        = "VENTAS";
    public static final String MOD_FACTURACION   = "FACTURACION";
    public static final String MOD_CAJA          = "CAJA";
    public static final String MOD_FINANZAS      = "FINANZAS";
    public static final String MOD_EVENTOS       = "EVENTOS";
    public static final String MOD_RESERVAS      = "RESERVAS";
    public static final String MOD_PROMOCIONES   = "PROMOCIONES";
    public static final String MOD_CONFIGURACION = "CONFIGURACION";
    public static final String MOD_COMERCIAL     = "COMERCIAL";
    public static final String MOD_CMS           = "CMS";
    public static final String MOD_CALENDARIO    = "CALENDARIO";
    public static final String MOD_MENSAJES      = "MENSAJES";

    // ── Acciones ─────────────────────────────────────────────────────────────
    public static final String ACCION_CREAR         = "CREAR";
    public static final String ACCION_ACTUALIZAR    = "ACTUALIZAR";
    public static final String ACCION_ELIMINAR      = "ELIMINAR";
    public static final String ACCION_LOGIN         = "LOGIN";
    public static final String ACCION_LOGOUT        = "LOGOUT";
    public static final String ACCION_LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String ACCION_BLOQUEO       = "BLOQUEO_CUENTA";
    public static final String ACCION_CONFIRMAR     = "CONFIRMAR";
    public static final String ACCION_CANCELAR      = "CANCELAR";
    public static final String ACCION_REPROGRAMAR   = "REPROGRAMAR";
    public static final String ACCION_FIRMAR        = "FIRMAR";
    public static final String ACCION_ABRIR         = "ABRIR";
    public static final String ACCION_CERRAR        = "CERRAR";
    public static final String ACCION_ARQUEO        = "ARQUEO";
    public static final String ACCION_EMITIR        = "EMITIR";
    public static final String ACCION_ANULAR        = "ANULAR";
    public static final String ACCION_APROBAR       = "APROBAR";
    public static final String ACCION_RECHAZAR      = "RECHAZAR";
    public static final String ACCION_ACTIVAR       = "ACTIVAR";
    public static final String ACCION_DESACTIVAR    = "DESACTIVAR";
    public static final String ACCION_RESPONDER     = "RESPONDER";
    public static final String ACCION_MARCAR_SPAM   = "MARCAR_SPAM";

    // ── Niveles ──────────────────────────────────────────────────────────────
    public static final String NIVEL_INFO     = "INFO";
    public static final String NIVEL_WARNING  = "WARNING";
    public static final String NIVEL_ERROR    = "ERROR";
    public static final String NIVEL_CRITICAL = "CRITICAL";

    // ── Resultados ───────────────────────────────────────────────────────────
    public static final String RESULTADO_EXITOSO = "EXITOSO";
    public static final String RESULTADO_FALLIDO = "FALLIDO";
    public static final String RESULTADO_PARCIAL = "PARCIAL";
}
