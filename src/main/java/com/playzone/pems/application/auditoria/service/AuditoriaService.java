package com.playzone.pems.application.auditoria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import com.playzone.pems.domain.auditoria.repository.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService implements RegistrarLogUseCase {

    private final LogAuditoriaRepository logRepository;
    private final ObjectMapper           objectMapper;

    @Async("asyncExecutor")
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutar(Command command) {
        try {
            logRepository.save(construir(command));
        } catch (Exception e) {
            log.error("Error al registrar log de auditoría: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void ejecutarSincrono(Command command) {
        logRepository.save(construir(command));
    }

    private LogAuditoria construir(Command command) {
        return LogAuditoria.builder()
                .idUsuarioAdmin(command.idUsuarioAdmin())
                .accion(command.accion())
                .modulo(command.modulo())
                .entidadAfectada(command.entidadAfectada())
                .idEntidad(command.idEntidad())
                .valorAnterior(toJson(command.valorAnterior()))
                .valorNuevo(toJson(command.valorNuevo()))
                .descripcion(command.descripcion())
                .ipOrigen(command.ipOrigen())
                .userAgent(command.userAgent())
                .nivel(command.nivel())
                .resultado(command.resultado())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
