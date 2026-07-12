package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.ClientePerfilEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientePerfilJpaRepository extends JpaRepository<ClientePerfilEntity, Long> {

    Optional<ClientePerfilEntity> findByUsuarioIdAndDeletedAtIsNull(UUID usuarioId);

    Optional<ClientePerfilEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<ClientePerfilEntity> findByCorreoAndDeletedAtIsNull(String correo);

    Optional<ClientePerfilEntity> findByTipoDocumentoCodigoAndNumeroDocumentoAndDeletedAtIsNull(
            String tipoDocumentoCodigo, String numeroDocumento);

    List<ClientePerfilEntity> findByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("""
        SELECT c FROM ClientePerfilEntity c
        WHERE
            (CAST(:search AS string) IS NULL OR
            LOWER(c.nombreCompleto)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
            LOWER(c.correo)          LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
            LOWER(c.telefono)        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
            c.numeroDocumento        LIKE CONCAT('%', CAST(:search AS string), '%'))
            AND (:esVip IS NULL OR c.esVip = :esVip)
            AND (:filterActivos   IS NULL OR c.deletedAt IS NULL)
            AND (:filterInactivos IS NULL OR c.deletedAt IS NOT NULL)
            AND (:frecuente IS NULL OR c.contadorVisitas >= :minVisitas)
            AND (:aceptaComunicaciones IS NULL OR c.aceptaComunicaciones = :aceptaComunicaciones)
            AND (CAST(:segmentoCodigo AS string) IS NULL OR c.segmentoCodigo = :segmentoCodigo)
            AND (CAST(:origen AS string) IS NULL OR c.origen = :origen)
        """)
    Page<ClientePerfilEntity> buscarPaginado(
            @Param("search")               String  search,
            @Param("esVip")                Boolean esVip,
            @Param("filterActivos")        Boolean filterActivos,
            @Param("filterInactivos")      Boolean filterInactivos,
            @Param("frecuente")            Boolean frecuente,
            @Param("minVisitas")           int     minVisitas,
            @Param("aceptaComunicaciones") Boolean aceptaComunicaciones,
            @Param("segmentoCodigo")       String  segmentoCodigo,
            @Param("origen")               String  origen,
            Pageable pageable);

    @Query("""
        SELECT c FROM ClientePerfilEntity c
        WHERE c.deletedAt IS NULL
          AND c.aceptaComunicaciones = true
          AND c.correo IS NOT NULL
          AND (:soloVip         IS NULL OR (:soloVip         = true AND c.esVip = true))
          AND (:soloFrecuentes  IS NULL OR (:soloFrecuentes  = true AND c.contadorVisitas >= :minVisitas))
          AND (:soloNuevos      IS NULL OR (:soloNuevos      = true AND c.segmentoCodigo = 'NUEVO'))
          AND (:soloInactivos   IS NULL OR (:soloInactivos   = true AND c.segmentoCodigo = 'INACTIVO'))
          AND (:soloCorporativos IS NULL OR (:soloCorporativos = true AND c.tipoDocumentoCodigo = 'RUC'))
          AND (:soloPresenciales IS NULL OR (:soloPresenciales = true AND c.origen IN ('PRESENCIAL','ADMIN')))
        """)
    List<ClientePerfilEntity> buscarDestinatariosCampana(
            @Param("soloVip")          Boolean soloVip,
            @Param("soloFrecuentes")   Boolean soloFrecuentes,
            @Param("soloNuevos")       Boolean soloNuevos,
            @Param("soloInactivos")    Boolean soloInactivos,
            @Param("soloCorporativos") Boolean soloCorporativos,
            @Param("soloPresenciales") Boolean soloPresenciales,
            @Param("minVisitas")       int     minVisitas);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.contadorVisitas = c.contadorVisitas + 1 WHERE c.id = :id")
    void incrementarContadorVisitas(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.ultimaVisitaAt = :cuando WHERE c.id = :id")
    void actualizarUltimaVisita(@Param("id") Long id, @Param("cuando") OffsetDateTime cuando);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.segmentoCodigo = :segmentoCodigo WHERE c.id = :id")
    void actualizarSegmento(@Param("id") Long id, @Param("segmentoCodigo") String segmentoCodigo);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.esVip = true, c.descuentoVip = :descuento WHERE c.id = :id")
    void marcarComoVip(@Param("id") Long id, @Param("descuento") BigDecimal descuento);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.esVip = false, c.descuentoVip = null WHERE c.id = :id")
    void quitarVip(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.deletedAt = :ahora WHERE c.id = :id")
    void desactivar(@Param("id") Long id, @Param("ahora") OffsetDateTime ahora);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.deletedAt = null WHERE c.id = :id")
    void reactivar(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.aceptaComunicaciones = false WHERE c.id = :id")
    void desactivarComunicaciones(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ClientePerfilEntity c SET c.totalGastado = c.totalGastado + :monto WHERE c.id = :id")
    void sumarTotalGastado(@Param("id") Long id, @Param("monto") BigDecimal monto);
}
