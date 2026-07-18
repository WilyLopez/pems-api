package com.playzone.pems.application.auditoria.port.in;

import com.playzone.pems.application.auditoria.AuditoriaConstants;

import java.util.UUID;

public interface RegistrarLogUseCase {

    record Command(
            UUID   idUsuarioAdmin,
            String accion,
            String modulo,
            String entidadAfectada,
            Long   idEntidad,
            Object valorAnterior,
            Object valorNuevo,
            String descripcion,
            String ipOrigen,
            String userAgent,
            String nivel,
            String resultado
    ) {
        public Command {
            if (nivel     == null) nivel     = AuditoriaConstants.NIVEL_INFO;
            if (resultado == null) resultado = AuditoriaConstants.RESULTADO_EXITOSO;
        }
    }

    void ejecutar(Command command);

    void ejecutarSincrono(Command command);
}
