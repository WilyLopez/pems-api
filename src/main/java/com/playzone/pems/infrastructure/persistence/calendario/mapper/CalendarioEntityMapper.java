package com.playzone.pems.infrastructure.persistence.calendario.mapper;

import com.playzone.pems.domain.calendario.model.*;
import com.playzone.pems.infrastructure.persistence.calendario.entity.*;
import org.springframework.stereotype.Component;

@Component
public class CalendarioEntityMapper {

    public Turno toDomain(TurnoEntity e) {
        if (e == null) return null;
        return Turno.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .descripcion(e.getDescripcion())
                .horaInicio(e.getHoraInicio())
                .horaFin(e.getHoraFin())
                .build();
    }

    public Feriado toDomain(FeriadoEntity e) {
        if (e == null) return null;
        return Feriado.builder()
                .id(e.getId())
                .tipoFeriado(e.getTipoFeriado())
                .fecha(e.getFecha())
                .descripcion(e.getDescripcion())
                .anio(e.getAnio())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public BloqueCalendario toDomain(BloqueCalendarioEntity e) {
        if (e == null) return null;
        return BloqueCalendario.builder()
                .id(e.getId())
                .idSede(e.getSede().getId())
                .fechaInicio(e.getFechaInicio())
                .fechaFin(e.getFechaFin())
                .tipoBloqueo(e.getTipoBloqueo())
                .motivo(e.getMotivo())
                .activo(e.isActivo())
                .idUsuarioCreador(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public Tarifa toDomain(TarifaEntity e) {
        if (e == null) return null;
        return Tarifa.builder()
                .id(e.getId())
                .idSede(e.getSede().getId())
                .tipoDia(e.getTipoDia())
                .precio(e.getPrecio())
                .duracionMinutos(e.getDuracionMinutos())
                .vigenciaDesde(e.getVigenciaDesde())
                .vigenciaHasta(e.getVigenciaHasta())
                .activo(e.isActivo())
                .createdAt(e.getCreatedAt())
                .build();
    }
}