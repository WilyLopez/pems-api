package com.playzone.pems.infrastructure.persistence.venta.mapper;

import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.infrastructure.persistence.venta.entity.VentaPagoEntity;
import org.springframework.stereotype.Component;

@Component
public class VentaPagoEntityMapper {

    public VentaPago toDomain(VentaPagoEntity e) {
        return VentaPago.builder()
                .id(e.getId())
                .ventaId(e.getVentaId())
                .medioPagoCodigo(e.getMedioPagoCodigo())
                .monto(e.getMonto())
                .referencia(e.getReferencia())
                .motivoRechazo(e.getMotivoRechazo())
                .esValidado(e.isEsValidado())
                .validadoPor(e.getValidadoPor())
                .validadoAt(e.getValidadoAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public VentaPagoEntity toEntity(VentaPago d) {
        return VentaPagoEntity.builder()
                .id(d.getId())
                .ventaId(d.getVentaId())
                .medioPagoCodigo(d.getMedioPagoCodigo())
                .monto(d.getMonto())
                .referencia(d.getReferencia())
                .motivoRechazo(d.getMotivoRechazo())
                .esValidado(d.isEsValidado())
                .validadoPor(d.getValidadoPor())
                .validadoAt(d.getValidadoAt())
                .build();
    }
}
