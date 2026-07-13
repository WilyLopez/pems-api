package com.playzone.pems.infrastructure.persistence.venta.mapper;

import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import com.playzone.pems.infrastructure.persistence.venta.entity.VentaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VentaEntityMapper {

    public Venta toDomain(VentaEntity e) {
        if (e == null) return null;
        return Venta.builder()
                .id(e.getId())
                .idSede(e.getSede().getId())
                .clienteId(e.getClienteId())
                .eventoId(e.getEventoId())
                .tipo(e.getTipo())
                .canalCodigo(e.getCanalCodigo())
                .fechaVisita(e.getFechaVisita())
                .nombreAcompanante(e.getNombreAcompanante())
                .dniAcompanante(e.getDniAcompanante())
                .tipoDocumentoAcompanante(e.getTipoDocumentoAcompanante())
                .telefonoAcompanante(e.getTelefonoAcompanante())
                .promocionId(e.getPromocionId())
                .subtotal(e.getSubtotal())
                .descuento(e.getDescuento())
                .total(e.getTotal())
                .efectivoRecibido(e.getEfectivoRecibido())
                .vuelto(e.getVuelto())
                .actaFirmada(e.isActaFirmada())
                .esAnticipada(e.isEsAnticipada())
                .impreso(e.isImpreso())
                .enviadoCorreo(e.isEnviadoCorreo())
                .descargado(e.isDescargado())
                .notas(e.getNotas())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt() : null)
                .createdBy(e.getCreatedBy())
                .deletedAt(e.getDeletedAt() != null ? e.getDeletedAt() : null)
                .build();
    }

    public VentaEntity toEntity(Venta d, SedeEntity sede) {
        if (d == null) return null;
        return VentaEntity.builder()
                .id(d.getId())
                .sede(sede)
                .clienteId(d.getClienteId())
                .eventoId(d.getEventoId())
                .tipo(d.getTipo())
                .canalCodigo(d.getCanalCodigo())
                .fechaVisita(d.getFechaVisita())
                .nombreAcompanante(d.getNombreAcompanante())
                .dniAcompanante(d.getDniAcompanante())
                .tipoDocumentoAcompanante(d.getTipoDocumentoAcompanante())
                .telefonoAcompanante(d.getTelefonoAcompanante())
                .promocionId(d.getPromocionId())
                .subtotal(d.getSubtotal())
                .descuento(d.getDescuento() != null ? d.getDescuento() : BigDecimal.ZERO)
                .total(d.getTotal())
                .efectivoRecibido(d.getEfectivoRecibido() != null ? d.getEfectivoRecibido() : BigDecimal.ZERO)
                .vuelto(d.getVuelto() != null ? d.getVuelto() : BigDecimal.ZERO)
                .actaFirmada(d.isActaFirmada())
                .esAnticipada(d.isEsAnticipada())
                .impreso(d.isImpreso())
                .enviadoCorreo(d.isEnviadoCorreo())
                .descargado(d.isDescargado())
                .notas(d.getNotas())
                .createdBy(d.getCreatedBy())
                .build();
    }

}
