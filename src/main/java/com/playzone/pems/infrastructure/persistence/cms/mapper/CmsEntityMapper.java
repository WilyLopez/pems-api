package com.playzone.pems.infrastructure.persistence.cms.mapper;

import com.playzone.pems.domain.cms.model.*;
import com.playzone.pems.infrastructure.persistence.cms.entity.*;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import org.springframework.stereotype.Component;

@Component
public class CmsEntityMapper {

    public Banner toDomain(BannerEntity e) {
        if (e == null) return null;
        return Banner.builder()
                .id(e.getId())
                .idSede(e.getSede() != null ? e.getSede().getId() : null)
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .imagenUrl(e.getImagenUrl())
                .imagenMovilUrl(e.getImagenMovilUrl())
                .enlaceDestino(e.getEnlaceDestino())
                .textoBoton(e.getTextoBoton())
                .colorOverlay(e.getColorOverlay())
                .tipoBanner(e.getTipoBanner())
                .fechaInicio(e.getFechaInicio())
                .fechaFin(e.getFechaFin())
                .activo(e.isActivo())
                .orden(e.getOrden())
                .prioridad(e.getPrioridad())
                .soloMovil(e.isSoloMovil())
                .soloDesktop(e.isSoloDesktop())
                .fechaCreacion(e.getCreatedAt())
                .build();
    }

    public BannerEntity toEntity(Banner d, SedeEntity sede) {
        if (d == null) return null;
        return BannerEntity.builder()
                .id(d.getId())
                .sede(sede)
                .titulo(d.getTitulo())
                .descripcion(d.getDescripcion())
                .imagenUrl(d.getImagenUrl())
                .imagenMovilUrl(d.getImagenMovilUrl())
                .enlaceDestino(d.getEnlaceDestino())
                .textoBoton(d.getTextoBoton())
                .colorOverlay(d.getColorOverlay())
                .tipoBanner(d.getTipoBanner())
                .fechaInicio(d.getFechaInicio())
                .fechaFin(d.getFechaFin())
                .activo(d.isActivo())
                .orden(d.getOrden())
                .prioridad(d.getPrioridad())
                .soloMovil(d.isSoloMovil())
                .soloDesktop(d.isSoloDesktop())
                .build();
    }

    public ContenidoLegal toDomain(ContenidoLegalEntity e) {
        if (e == null) return null;
        return ContenidoLegal.builder()
                .id(e.getId())
                .tipo(e.getTipo())
                .titulo(e.getTitulo())
                .contenido(e.getContenido())
                .version(e.getVersion())
                .activo(e.isActivo())
                .fechaActualizacion(e.getUpdatedAt())
                .build();
    }

    public ContenidoLegalEntity toEntity(ContenidoLegal d) {
        if (d == null) return null;
        return ContenidoLegalEntity.builder()
                .id(d.getId())
                .tipo(d.getTipo())
                .titulo(d.getTitulo())
                .contenido(d.getContenido())
                .version(d.getVersion())
                .activo(d.isActivo())
                .build();
    }

    public TipoLegal toDomain(TipoLegalEntity e) {
        if (e == null) return null;
        return TipoLegal.builder()
                .codigo(e.getCodigo())
                .etiqueta(e.getEtiqueta())
                .slug(e.getSlug())
                .orden(e.getOrden())
                .esSistema(e.isEsSistema())
                .requerido(e.isRequerido())
                .visibleFooter(e.isVisibleFooter())
                .build();
    }

    public ContenidoLegalHistorial toDomain(ContenidoLegalHistorialEntity e) {
        if (e == null) return null;
        return ContenidoLegalHistorial.builder()
                .id(e.getId())
                .legalId(e.getLegalId())
                .tipo(e.getTipo())
                .titulo(e.getTitulo())
                .contenido(e.getContenido())
                .version(e.getVersion())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public ContenidoWeb toDomain(ContenidoWebEntity e) {
        if (e == null) return null;
        return ContenidoWeb.builder()
                .id(e.getId())
                .seccionCodigo(e.getSeccionCodigo())
                .tipoContenidoCodigo(e.getTipoContenidoCodigo())
                .clave(e.getClave())
                .valorEs(e.getValorEs())
                .valorEn(e.getValorEn())
                .imagenUrl(e.getImagenUrl())
                .descripcion(e.getDescripcion())
                .ordenVisualizacion(e.getOrden())
                .visible(e.isVisible())
                .version(e.getVersion())
                .metadatos(e.getMetadatos())
                .idUsuarioEditor(e.getUpdatedBy())
                .fechaActualizacion(e.getUpdatedAt())
                .build();
    }

    public ContenidoWebEntity toEntity(ContenidoWeb d) {
        if (d == null) return null;
        return ContenidoWebEntity.builder()
                .id(d.getId())
                .seccionCodigo(d.getSeccionCodigo())
                .tipoContenidoCodigo(d.getTipoContenidoCodigo())
                .clave(d.getClave())
                .valorEs(d.getValorEs())
                .valorEn(d.getValorEn())
                .imagenUrl(d.getImagenUrl())
                .descripcion(d.getDescripcion())
                .orden(d.getOrdenVisualizacion())
                .visible(d.isVisible())
                .version(d.getVersion())
                .metadatos(d.getMetadatos())
                .updatedBy(d.getIdUsuarioEditor())
                .build();
    }

    public Faq toDomain(FaqEntity e) {
        if (e == null) return null;
        return Faq.builder()
                .id(e.getId())
                .pregunta(e.getPregunta())
                .respuesta(e.getRespuesta())
                .ordenVisualizacion(e.getOrden())
                .visible(e.isVisible())
                .idUsuarioEditor(e.getUpdatedBy())
                .fechaActualizacion(e.getUpdatedAt())
                .build();
    }

    public FaqEntity toEntity(Faq d) {
        if (d == null) return null;
        return FaqEntity.builder()
                .id(d.getId())
                .pregunta(d.getPregunta())
                .respuesta(d.getRespuesta())
                .orden(d.getOrdenVisualizacion())
                .visible(d.isVisible())
                .updatedBy(d.getIdUsuarioEditor())
                .build();
    }

    public Resena toDomain(ResenaEntity e) {
        if (e == null) return null;
        return Resena.builder()
                .id(e.getId())
                .idCliente(e.getClienteId())
                .idEventoPrivado(e.getEventoId())
                .nombreAutor(e.getNombreAutor())
                .contenido(e.getContenido())
                .calificacion(e.getCalificacion())
                .aprobada(e.isAprobada())
                .fotoUrl(e.getFotoUrl())
                .respuestaAdmin(e.getRespuestaAdmin())
                .fechaRespuesta(e.getFechaRespuesta())
                .destacada(e.isDestacada())
                .mostrarHome(e.isMostrarHome())
                .idUsuarioAprueba(e.getAprobadaPor())
                .fechaCreacion(e.getCreatedAt())
                .build();
    }

    public ResenaEntity toEntity(Resena d) {
        if (d == null) return null;
        return ResenaEntity.builder()
                .id(d.getId())
                .clienteId(d.getIdCliente())
                .eventoId(d.getIdEventoPrivado())
                .nombreAutor(d.getNombreAutor())
                .contenido(d.getContenido())
                .calificacion(d.getCalificacion())
                .aprobada(d.isAprobada())
                .fotoUrl(d.getFotoUrl())
                .respuestaAdmin(d.getRespuestaAdmin())
                .fechaRespuesta(d.getFechaRespuesta())
                .destacada(d.isDestacada())
                .mostrarHome(d.isMostrarHome())
                .aprobadaPor(d.getIdUsuarioAprueba())
                .build();
    }

    public ImagenGaleria toDomain(ImagenGaleriaEntity e) {
        if (e == null) return null;
        return ImagenGaleria.builder()
                .id(e.getId())
                .idSede(e.getSede() != null ? e.getSede().getId() : null)
                .urlImagen(e.getUrlImagen())
                .altTexto(e.getAltTexto())
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .categoriaImagen(e.getCategoriaImagen())
                .tipoMime(e.getTipoMime())
                .tamanioBytes(e.getTamanioBytes())
                .ordenVisualizacion(e.getOrdenVisualizacion())
                .activo(e.isActivo())
                .destacada(e.isDestacada())
                .eliminada(e.getDeletedAt() != null)
                .idUsuarioSubio(e.getSubidaPor())
                .fechaSubida(e.getFechaSubida())
                .build();
    }

    public ImagenGaleriaEntity toEntity(ImagenGaleria d, SedeEntity sede) {
        if (d == null) return null;
        return ImagenGaleriaEntity.builder()
                .id(d.getId())
                .sede(sede)
                .urlImagen(d.getUrlImagen())
                .altTexto(d.getAltTexto())
                .titulo(d.getTitulo())
                .descripcion(d.getDescripcion())
                .categoriaImagen(d.getCategoriaImagen())
                .tipoMime(d.getTipoMime())
                .tamanioBytes(d.getTamanioBytes())
                .ordenVisualizacion(d.getOrdenVisualizacion())
                .activo(d.isActivo())
                .destacada(d.isDestacada())
                .subidaPor(d.getIdUsuarioSubio())
                .build();
    }

    public SeccionWeb toDomain(SeccionWebEntity e) {
        if (e == null) return null;
        return SeccionWeb.builder()
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .esSistema(e.isEsSistema())
                .activo(e.isActivo())
                .orden(e.getOrden())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public SeccionWebEntity toEntity(SeccionWeb d) {
        if (d == null) return null;
        return SeccionWebEntity.builder()
                .codigo(d.getCodigo())
                .nombre(d.getNombre())
                .descripcion(d.getDescripcion())
                .esSistema(d.isEsSistema())
                .activo(d.isActivo())
                .orden(d.getOrden())
                .build();
    }

    public TipoContenido toDomain(TipoContenidoEntity e) {
        if (e == null) return null;
        return TipoContenido.builder()
                .codigo(e.getCodigo())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .esSistema(e.isEsSistema())
                .activo(e.isActivo())
                .orden(e.getOrden())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public TipoContenidoEntity toEntity(TipoContenido d) {
        if (d == null) return null;
        return TipoContenidoEntity.builder()
                .codigo(d.getCodigo())
                .nombre(d.getNombre())
                .descripcion(d.getDescripcion())
                .esSistema(d.isEsSistema())
                .activo(d.isActivo())
                .orden(d.getOrden())
                .build();
    }

    public ConfiguracionPublica toDomain(ConfiguracionPublicaEntity e) {
        if (e == null) return null;
        return ConfiguracionPublica.builder()
                .id(e.getId())
                .nombreNegocio(e.getNombreNegocio())
                .slogan(e.getSlogan())
                .logoPath(e.getLogoPath())
                .faviconPath(e.getFaviconPath())
                .logoSecundarioPath(e.getLogoSecundarioPath())
                .mascota1Path(e.getMascota1Path())
                .mascota2Path(e.getMascota2Path())
                .telefono(e.getTelefono())
                .telefonoSecundario(e.getTelefonoSecundario())
                .whatsapp(e.getWhatsapp())
                .correo(e.getCorreo())
                .correoSecundario(e.getCorreoSecundario())
                .facebookUrl(e.getFacebookUrl())
                .instagramUrl(e.getInstagramUrl())
                .tiktokUrl(e.getTiktokUrl())
                .youtubeUrl(e.getYoutubeUrl())
                .copyrightTexto(e.getCopyrightTexto())
                .metricasNegocio(e.getMetricasNegocio())
                .esMantenimientoActivo(e.isEsMantenimientoActivo())
                .mensajeMantenimiento(e.getMensajeMantenimiento())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    public ConfiguracionPublicaEntity toEntity(ConfiguracionPublica d) {
        if (d == null) return null;
        return ConfiguracionPublicaEntity.builder()
                .id(d.getId())
                .nombreNegocio(d.getNombreNegocio())
                .slogan(d.getSlogan())
                .logoPath(d.getLogoPath())
                .faviconPath(d.getFaviconPath())
                .logoSecundarioPath(d.getLogoSecundarioPath())
                .mascota1Path(d.getMascota1Path())
                .mascota2Path(d.getMascota2Path())
                .telefono(d.getTelefono())
                .telefonoSecundario(d.getTelefonoSecundario())
                .whatsapp(d.getWhatsapp())
                .correo(d.getCorreo())
                .correoSecundario(d.getCorreoSecundario())
                .facebookUrl(d.getFacebookUrl())
                .instagramUrl(d.getInstagramUrl())
                .tiktokUrl(d.getTiktokUrl())
                .youtubeUrl(d.getYoutubeUrl())
                .copyrightTexto(d.getCopyrightTexto())
                .metricasNegocio(d.getMetricasNegocio())
                .esMantenimientoActivo(d.isEsMantenimientoActivo())
                .mensajeMantenimiento(d.getMensajeMantenimiento())
                .updatedBy(d.getUpdatedBy())
                .build();
    }

    public MensajeContacto toDomain(MensajeContactoEntity e) {
        if (e == null) return null;
        return MensajeContacto.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .correo(e.getCorreo())
                .telefono(e.getTelefono())
                .asunto(e.getAsunto())
                .mensaje(e.getMensaje())
                .estado(e.getEstado())
                .respuesta(e.getRespuesta())
                .respondidoPor(e.getRespondidoPor())
                .respondidoAt(e.getRespondidoAt())
                .ipOrigen(e.getIpOrigen())
                .userAgent(e.getUserAgent())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public MensajeContactoEntity toEntity(MensajeContacto d) {
        if (d == null) return null;
        return MensajeContactoEntity.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .correo(d.getCorreo())
                .telefono(d.getTelefono())
                .asunto(d.getAsunto())
                .mensaje(d.getMensaje())
                .estado(d.getEstado())
                .respuesta(d.getRespuesta())
                .respondidoPor(d.getRespondidoPor())
                .respondidoAt(d.getRespondidoAt())
                .ipOrigen(d.getIpOrigen())
                .userAgent(d.getUserAgent())
                .build();
    }
}
