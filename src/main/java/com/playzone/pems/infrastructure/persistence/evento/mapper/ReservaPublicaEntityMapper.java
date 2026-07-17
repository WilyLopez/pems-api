package com.playzone.pems.infrastructure.persistence.evento.mapper;

import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.infrastructure.persistence.evento.entity.ReservaPublicaEntity;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class ReservaPublicaEntityMapper {

    public ReservaPublica toDomain(ReservaPublicaEntity e) {
        if (e == null) return null;
        return ReservaPublica.builder()
                .id(e.getId())
                .ventaId(e.getVentaId())
                .idCliente(e.getClienteId())
                .idSede(e.getSede().getId())
                .estado(e.getEstado())
                .canalReserva(e.getCanalReserva())
                .tipoDia(e.getTipoDia())
                .idReservaOriginal(e.getReservaOriginal() != null ? e.getReservaOriginal().getId() : null)
                .esReprogramacion(e.isEsReprogramacion())
                .vecesReprogramada(e.getVecesReprogramada())
                .fechaEvento(e.getFechaEvento())
                .numeroTicket(e.getNumeroTicket())
                .precioHistorico(e.getPrecioHistorico())
                .descuentoAplicado(e.getDescuentoAplicado())
                .totalPagado(e.getTotalPagado())
                .nombreNino(e.getNombreNino())
                .edadNino(e.getEdadNino())
                .nombreAcompanante(e.getNombreAcompanante())
                .dniAcompanante(e.getDniAcompanante())
                .tipoDocumentoAcompanante(e.getTipoDocumentoAcompanante())
                .firmoConsentimiento(e.isFirmoConsentimiento())
                .motivoCancelacion(e.getMotivoCancelacion())
                .ingresado(e.isIngresado())
                .ingresoAt(e.getIngresoAt())
                .duracionHistoricaMinutos(e.getDuracionHistoricaMinutos())
                .permanenciaFinAt(e.getPermanenciaFinAt())
                .salidaRealAt(e.getSalidaRealAt())
                .codigoQr(e.getCodigoQr())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .deletedAt(e.getDeletedAt())
                .build();
    }

    public ReservaPublicaEntity toEntity(ReservaPublica d,
                                          SedeEntity sede,
                                          ReservaPublicaEntity reservaOriginal) {
        if (d == null) return null;
        return ReservaPublicaEntity.builder()
                .id(d.getId())
                .ventaId(d.getVentaId())
                .clienteId(d.getIdCliente())
                .sede(sede)
                .estado(d.getEstado())
                .canalReserva(d.getCanalReserva())
                .tipoDia(d.getTipoDia())
                .reservaOriginal(reservaOriginal)
                .esReprogramacion(d.isEsReprogramacion())
                .vecesReprogramada(d.getVecesReprogramada())
                .fechaEvento(d.getFechaEvento())
                .numeroTicket(d.getNumeroTicket())
                .precioHistorico(d.getPrecioHistorico())

                .descuentoAplicado(d.getDescuentoAplicado())
                .totalPagado(d.getTotalPagado())
                .nombreNino(d.getNombreNino())
                .edadNino(d.getEdadNino())
                .nombreAcompanante(d.getNombreAcompanante())
                .dniAcompanante(d.getDniAcompanante())
                .tipoDocumentoAcompanante(d.getTipoDocumentoAcompanante())
                .firmoConsentimiento(d.isFirmoConsentimiento())
                .motivoCancelacion(d.getMotivoCancelacion())
                .ingresado(d.isIngresado())
                .ingresoAt(d.getIngresoAt())
                .duracionHistoricaMinutos(d.getDuracionHistoricaMinutos())
                .permanenciaFinAt(d.getPermanenciaFinAt())
                .salidaRealAt(d.getSalidaRealAt())
                .codigoQr(d.getCodigoQr())
                .createdBy(d.getCreatedBy())
                .updatedBy(d.getUpdatedBy())
                .deletedAt(d.getDeletedAt())
                .build();
    }
}
