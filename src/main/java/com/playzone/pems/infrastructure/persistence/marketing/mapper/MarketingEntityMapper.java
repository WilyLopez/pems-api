package com.playzone.pems.infrastructure.persistence.marketing.mapper;

import com.playzone.pems.domain.marketing.model.CampanaEmail;
import com.playzone.pems.domain.marketing.model.EnvioEmail;
import com.playzone.pems.domain.marketing.model.PlantillaEmail;
import com.playzone.pems.domain.marketing.model.TipoEmail;
import com.playzone.pems.infrastructure.persistence.marketing.entity.CampanaEmailEntity;
import com.playzone.pems.infrastructure.persistence.marketing.entity.EnvioEmailEntity;
import com.playzone.pems.infrastructure.persistence.marketing.entity.PlantillaEmailEntity;
import com.playzone.pems.infrastructure.persistence.marketing.entity.TipoEmailEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class MarketingEntityMapper {

    public TipoEmail toDomain(TipoEmailEntity e) {
        return TipoEmail.builder()
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .esSistema(e.isEsSistema())
                .orden(e.getOrden())
                .activo(e.isActivo())
                .build();
    }

    public PlantillaEmail toDomain(PlantillaEmailEntity e) {
        return PlantillaEmail.builder()
                .id(e.getId())
                .tipoEmailCodigo(e.getTipoEmailCodigo())
                .tipoEmailNombre(null)
                .nombre(e.getNombre())
                .asunto(e.getAsunto())
                .contenidoHtml(e.getContenidoHtml())
                .contenidoFallback(e.getContenidoFallback())
                .variablesPermitidas(e.getVariablesPermitidas())
                .contenidoBloques(e.getContenidoBloques())
                .activa(e.isEsActiva())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .fechaActualizacion(e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null)
                .build();
    }

    public CampanaEmail toDomain(CampanaEmailEntity e) {
        return CampanaEmail.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .idPlantillaEmail(e.getPlantillaId())
                .plantillaNombre(null)
                .estado(e.getEstado())
                .fechaProgramada(e.getFechaProgramada() != null ? e.getFechaProgramada().toInstant() : null)
                .totalDestinatarios(e.getTotalDestinatarios())
                .totalEnviados(e.getTotalEnviados())
                .totalFallidos(e.getTotalFallidos())
                .createdBy(e.getCreatedBy())
                .enviadaPor(e.getEnviadaPor())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null)
                .build();
    }

    public EnvioEmail toDomain(EnvioEmailEntity e) {
        return EnvioEmail.builder()
                .id(e.getId())
                .idCampanaEmail(e.getCampanaId())
                .idCliente(e.getClienteId())
                .destinatario(e.getDestinatario())
                .asunto(e.getAsunto())
                .cuerpoHtml(e.getCuerpoHtml())
                .estado(e.getEstado())
                .intentos(e.getIntentos())
                .fechaEnvio(e.getEnviadoAt() != null ? e.getEnviadoAt().toInstant() : null)
                .mensajeError(e.getMensajeError())
                .proveedorMensajeId(e.getProveedorMensajeId())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null)
                .build();
    }

    public EnvioEmailEntity toEntity(EnvioEmail d) {
        return EnvioEmailEntity.builder()
                .id(d.getId())
                .campanaId(d.getIdCampanaEmail())
                .clienteId(d.getIdCliente())
                .destinatario(d.getDestinatario())
                .asunto(d.getAsunto())
                .cuerpoHtml(d.getCuerpoHtml())
                .estado(d.getEstado())
                .intentos(d.getIntentos())
                .enviadoAt(d.getFechaEnvio() != null ? d.getFechaEnvio().atOffset(ZoneOffset.UTC) : null)
                .mensajeError(d.getMensajeError())
                .proveedorMensajeId(d.getProveedorMensajeId())
                .build();
    }
}
