package com.playzone.pems.application.auditoria.service;

import com.playzone.pems.application.auditoria.port.in.ObtenerAuditoriaUseCase;
import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import com.playzone.pems.domain.auditoria.repository.LogAuditoriaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObtenerAuditoriaService implements ObtenerAuditoriaUseCase {

    private final LogAuditoriaRepository logRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LogAuditoria> listarPorFiltros(FiltrosQuery filtros) {
        boolean hayFiltros = filtros.idUsuario() != null
                || filtros.modulo()    != null
                || filtros.accion()    != null
                || filtros.entidad()   != null
                || filtros.nivel()     != null
                || filtros.resultado() != null;

        var pageable = PaginacionUtil.construir(filtros.pagina(), filtros.tamano(), "fechaLog", "desc");

        if (hayFiltros) {
            return logRepository.findByFiltros(
                    filtros.desde(), filtros.hasta(),
                    filtros.idUsuario(), filtros.modulo(), filtros.accion(),
                    filtros.entidad(), filtros.nivel(), filtros.resultado(),
                    pageable);
        }
        return logRepository.findByFechasBetween(filtros.desde(), filtros.hasta(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public LogAuditoria obtenerPorId(Long id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LogAuditoria", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogAuditoria> listarPorUsuario(UUID idUsuario, int pagina, int tamano) {
        return logRepository.findByUsuario(
                idUsuario,
                PaginacionUtil.construir(pagina, tamano, "fechaLog", "desc"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogAuditoria> listarPorEntidad(String entidadAfectada, Long idEntidad, int pagina, int tamano) {
        return logRepository.findByEntidad(
                entidadAfectada, idEntidad,
                PaginacionUtil.construir(pagina, tamano, "fechaLog", "desc"));
    }
}
