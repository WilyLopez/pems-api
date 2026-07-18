package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.application.usuario.dto.query.CampanaDestinatariosQuery;
import com.playzone.pems.application.usuario.dto.query.ClientePerfilQuery;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.ClientePerfilEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.ClientePerfilJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientePerfilPersistenceAdapter implements ClientePerfilRepository {

    private final ClientePerfilJpaRepository jpa;

    @Override
    public Optional<ClientePerfil> buscarPorUsuarioId(UUID usuarioId) {
        return jpa.findByUsuarioIdAndDeletedAtIsNull(usuarioId).map(this::toDomain);
    }

    @Override
    public Optional<ClientePerfil> buscarPorUsuarioIdIncluyendoInactivos(UUID usuarioId) {
        return jpa.findByUsuarioId(usuarioId).map(this::toDomain);
    }

    @Override
    public Optional<ClientePerfil> buscarPorId(Long id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public Optional<ClientePerfil> buscarPorIdIncluyendoInactivos(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ClientePerfil> buscarPorCorreo(String correo) {
        return jpa.findByCorreoAndDeletedAtIsNull(correo).map(this::toDomain);
    }

    @Override
    public Optional<ClientePerfil> buscarPorDocumento(String tipoDocumentoCodigo, String numeroDocumento) {
        return jpa.findByTipoDocumentoCodigoAndNumeroDocumentoAndDeletedAtIsNull(tipoDocumentoCodigo, numeroDocumento)
                .map(this::toDomain);
    }

    @Override
    public List<ClientePerfil> buscarPorIds(List<Long> ids) {
        return jpa.findByIdInAndDeletedAtIsNull(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Page<ClientePerfil> listarPaginado(ClientePerfilQuery query, Pageable pageable) {
        Boolean filterActivos   = Boolean.TRUE.equals(query.getActivo())  ? Boolean.TRUE : null;
        Boolean filterInactivos = Boolean.FALSE.equals(query.getActivo()) ? Boolean.TRUE : null;
        return jpa.buscarPaginado(
                query.getSearch(),
                query.getEsVip(),
                filterActivos,
                filterInactivos,
                query.getFrecuente(),
                query.getMinVisitas(),
                query.getAceptaComunicaciones(),
                query.getSegmentoCodigo(),
                query.getOrigen(),
                pageable
        ).map(this::toDomain);
    }

    @Override
    @Transactional
    public ClientePerfil guardar(ClientePerfil cliente) {
        ClientePerfilEntity entity = toEntity(cliente);
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public void incrementarContadorVisitas(Long id) {
        jpa.incrementarContadorVisitas(id);
    }

    @Override
    @Transactional
    public void actualizarUltimaVisita(Long id, OffsetDateTime cuando) {
        jpa.actualizarUltimaVisita(id, cuando);
    }

    @Override
    @Transactional
    public void actualizarSegmento(Long id, String segmentoCodigo) {
        jpa.actualizarSegmento(id, segmentoCodigo);
    }

    @Override
    @Transactional
    public void marcarComoVip(Long id, BigDecimal descuento) {
        jpa.marcarComoVip(id, descuento);
    }

    @Override
    @Transactional
    public void quitarVip(Long id) {
        jpa.quitarVip(id);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        jpa.desactivar(id, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void reactivar(Long id) {
        jpa.reactivar(id);
    }

    @Override
    @Transactional
    public void desactivarComunicaciones(Long id) {
        jpa.desactivarComunicaciones(id);
    }

    @Override
    @Transactional
    public void sumarTotalGastado(Long id, BigDecimal monto) {
        jpa.sumarTotalGastado(id, monto);
    }

    @Override
    public List<ClientePerfil> buscarDestinatariosCampana(CampanaDestinatariosQuery query) {
        return jpa.buscarDestinatariosCampana(
                query.getSoloVip(),
                query.getSoloFrecuentes(),
                query.getSoloNuevos(),
                query.getSoloInactivos(),
                query.getSoloCorporativos(),
                query.getSoloPresenciales(),
                query.getMinVisitas()
        ).stream().map(this::toDomain).toList();
    }

    private ClientePerfil toDomain(ClientePerfilEntity e) {
        return ClientePerfil.builder()
                .id(e.getId())
                .usuarioId(e.getUsuarioId())
                .tipoDocumentoCodigo(e.getTipoDocumentoCodigo())
                .numeroDocumento(e.getNumeroDocumento())
                .nombres(e.getNombres())
                .apellidoPaterno(e.getApellidoPaterno())
                .apellidoMaterno(e.getApellidoMaterno())
                .correo(e.getCorreo())
                .telefono(e.getTelefono())
                .fechaNacimiento(e.getFechaNacimiento())
                .ruc(e.getRuc())
                .razonSocial(e.getRazonSocial())
                .direccionFiscal(e.getDireccionFiscal())
                .segmentoCodigo(e.getSegmentoCodigo())
                .esVip(e.isEsVip())
                .descuentoVip(e.getDescuentoVip())
                .aceptaComunicaciones(e.isAceptaComunicaciones())
                .fotoPerfilPath(e.getFotoPerfilPath())
                .observaciones(e.getObservaciones())
                .contadorVisitas(e.getContadorVisitas())
                .totalGastado(e.getTotalGastado())
                .origen(e.getOrigen())
                .ultimaVisitaAt(e.getUltimaVisitaAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .deletedAt(e.getDeletedAt())
                .build();
    }

    private ClientePerfilEntity toEntity(ClientePerfil d) {
        return ClientePerfilEntity.builder()
                .id(d.getId())
                .usuarioId(d.getUsuarioId())
                .tipoDocumentoCodigo(d.getTipoDocumentoCodigo())
                .numeroDocumento(d.getNumeroDocumento())
                .nombres(d.getNombres())
                .apellidoPaterno(d.getApellidoPaterno())
                .apellidoMaterno(d.getApellidoMaterno())
                .correo(d.getCorreo())
                .telefono(d.getTelefono())
                .fechaNacimiento(d.getFechaNacimiento())
                .ruc(d.getRuc())
                .razonSocial(d.getRazonSocial())
                .direccionFiscal(d.getDireccionFiscal())
                .segmentoCodigo(d.getSegmentoCodigo() != null ? d.getSegmentoCodigo() : "NUEVO")
                .esVip(d.isEsVip())
                .descuentoVip(d.getDescuentoVip())
                .aceptaComunicaciones(d.isAceptaComunicaciones())
                .fotoPerfilPath(d.getFotoPerfilPath())
                .observaciones(d.getObservaciones())
                .contadorVisitas(d.getContadorVisitas())
                .totalGastado(d.getTotalGastado() != null ? d.getTotalGastado() : BigDecimal.ZERO)
                .origen(d.getOrigen() != null ? d.getOrigen() : "ADMIN")
                .ultimaVisitaAt(d.getUltimaVisitaAt())
                .createdBy(d.getCreatedBy())
                .updatedBy(d.getUpdatedBy())
                .deletedAt(d.getDeletedAt())
                .build();
    }
}
