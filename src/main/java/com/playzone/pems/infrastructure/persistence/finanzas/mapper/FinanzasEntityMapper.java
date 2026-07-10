package com.playzone.pems.infrastructure.persistence.finanzas.mapper;

import com.playzone.pems.domain.finanzas.model.GastoEventoPrivado;
import com.playzone.pems.domain.finanzas.model.GastoOperativoDiario;
import com.playzone.pems.domain.finanzas.model.RegistroEgreso;
import com.playzone.pems.domain.finanzas.model.TipoEgreso;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.GastoEventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.GastoOperativoDiarioEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.RegistroEgresoEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.TipoEgresoEntity;
import org.springframework.stereotype.Component;

@Component
public class FinanzasEntityMapper {

    public TipoEgreso toDomain(TipoEgresoEntity e) {
        return TipoEgreso.builder()
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .categoria(e.getCategoria())
                .esSistema(e.isEsSistema())
                .orden(e.getOrden())
                .activo(e.isActivo())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt() : null)
                .build();
    }

    public TipoEgresoEntity toEntity(TipoEgreso d) {
        return TipoEgresoEntity.builder()
                .codigo(d.getCodigo())
                .nombre(d.getNombre())
                .descripcion(d.getDescripcion())
                .categoria(d.getCategoria())
                .activo(d.isActivo())
                .build();
    }

    public RegistroEgreso toDomain(RegistroEgresoEntity e) {
        return RegistroEgreso.builder()
                .id(e.getId())
                .tipoEgresoCodigo(e.getTipoCodigo())
                .idSede(e.getSede().getId())
                .monto(e.getMonto())
                .fecha(e.getFecha())
                .medioPago(e.getMedioPagoCodigo())
                .periodoAnio(e.getPeriodoAnio())
                .periodoMes(e.getPeriodoMes())
                .descripcion(e.getDescripcion())
                .comprobanteUrl(e.getComprobantePath())
                .naturaleza(e.getNaturaleza())
                .idRegistroAnulado(e.getRegistroAnuladoId())
                .estadoAprobacion(e.getEstadoAprobacion())
                .aprobadoPor(e.getAprobadoPor())
                .fechaAprobacion(e.getFechaAprobacion())
                .motivoRechazo(e.getMotivoRechazo())
                .esRecurrente(e.isEsRecurrente())
                .idUsuarioRegistra(e.getCreatedBy())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .build();
    }

    public GastoEventoPrivado toDomain(GastoEventoPrivadoEntity e) {
        return GastoEventoPrivado.builder()
                .id(e.getId())
                .idEventoPrivado(e.getEventoId())
                .descripcion(e.getDescripcion())
                .monto(e.getMonto())
                .comprobanteUrl(e.getComprobantePath())
                .idUsuarioRegistra(e.getCreatedBy())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .build();
    }

    public GastoOperativoDiario toDomain(GastoOperativoDiarioEntity e) {
        return GastoOperativoDiario.builder()
                .id(e.getId())
                .idSede(e.getSede().getId())
                .fecha(e.getFecha())
                .descripcion(e.getDescripcion())
                .monto(e.getMonto())
                .comprobanteUrl(e.getComprobantePath())
                .idUsuarioRegistra(e.getCreatedBy())
                .naturaleza(e.getNaturaleza())
                .idGastoAnulado(e.getGastoAnuladoId())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .build();
    }
}
