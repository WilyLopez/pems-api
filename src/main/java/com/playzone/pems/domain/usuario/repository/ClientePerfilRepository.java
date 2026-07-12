package com.playzone.pems.domain.usuario.repository;

import com.playzone.pems.application.usuario.dto.query.CampanaDestinatariosQuery;
import com.playzone.pems.application.usuario.dto.query.ClientePerfilQuery;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientePerfilRepository {

    Optional<ClientePerfil> buscarPorUsuarioId(UUID usuarioId);

    Optional<ClientePerfil> buscarPorId(Long id);

    Optional<ClientePerfil> buscarPorCorreo(String correo);

    Optional<ClientePerfil> buscarPorDocumento(String tipoDocumentoCodigo, String numeroDocumento);

    List<ClientePerfil> buscarPorIds(List<Long> ids);

    Page<ClientePerfil> listarPaginado(ClientePerfilQuery query, Pageable pageable);

    ClientePerfil guardar(ClientePerfil cliente);

    void incrementarContadorVisitas(Long id);

    void actualizarUltimaVisita(Long id, OffsetDateTime cuando);

    void actualizarSegmento(Long id, String segmentoCodigo);

    void marcarComoVip(Long id, BigDecimal descuento);

    void quitarVip(Long id);

    void desactivar(Long id);

    void reactivar(Long id);

    void desactivarComunicaciones(Long id);

    void sumarTotalGastado(Long id, BigDecimal monto);

    List<ClientePerfil> buscarDestinatariosCampana(CampanaDestinatariosQuery query);
}
