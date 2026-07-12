package com.playzone.pems.application.contrato.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.contrato.dto.command.CargarContratoCommand;
import com.playzone.pems.application.contrato.dto.query.ActividadContratoQuery;
import com.playzone.pems.application.contrato.dto.query.ArchivoContratoQuery;
import com.playzone.pems.application.contrato.dto.query.ContratoPageQuery;
import com.playzone.pems.application.contrato.dto.query.ContratoQuery;
import com.playzone.pems.application.contrato.port.in.CargarContratoUseCase;
import com.playzone.pems.application.contrato.port.in.DescargarContratoUseCase;
import com.playzone.pems.application.contrato.port.in.ListarContratosUseCase;
import com.playzone.pems.application.contrato.port.in.ObtenerContratoUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.contrato.exception.ContratoNotFoundException;
import com.playzone.pems.domain.contrato.model.ActividadContrato;
import com.playzone.pems.domain.contrato.model.Contrato;
import com.playzone.pems.domain.contrato.repository.ActividadContratoRepository;
import com.playzone.pems.domain.contrato.repository.ContratoRepository;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.shared.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContratoService
        implements CargarContratoUseCase,
                   DescargarContratoUseCase,
                   ObtenerContratoUseCase,
                   ListarContratosUseCase {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long   TAMANIO_MAXIMO_BYTES = 15L * 1024 * 1024;
    private static final String CONTENT_TYPE_PDF     = "application/pdf";
    private static final byte[] FIRMA_PDF            = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String ACCION_CARGADO       = "CARGADO";
    private static final String ACCION_REEMPLAZADO   = "REEMPLAZADO";

    private final ContratoRepository          contratoRepository;
    private final ActividadContratoRepository actividadRepository;
    private final StoragePort                 storagePort;
    private final String                      bucketPrivado;
    private final RegistrarLogUseCase         auditoria;
    private final CrearNotificacionPort       crearNotificacionPort;

    public ContratoService(ContratoRepository contratoRepository,
                           ActividadContratoRepository actividadRepository,
                           StoragePort storagePort,
                           @Value("${supabase.storage.bucket-privado}") String bucketPrivado,
                           RegistrarLogUseCase auditoria,
                           CrearNotificacionPort crearNotificacionPort) {
        this.contratoRepository    = contratoRepository;
        this.actividadRepository   = actividadRepository;
        this.storagePort           = storagePort;
        this.bucketPrivado         = bucketPrivado;
        this.auditoria             = auditoria;
        this.crearNotificacionPort = crearNotificacionPort;
    }

    @Override
    @Transactional
    public ContratoQuery ejecutar(CargarContratoCommand command) {
        validarArchivo(command.getArchivo(), command.getContentType());

        Optional<Contrato> existente = contratoRepository.findByEventoPrivado(command.getIdEventoPrivado());

        String key = "contratos/contrato_" + command.getIdEventoPrivado() + "_"
                + LocalDateTime.now().format(FMT) + ".pdf";
        String url = storagePort.upload(bucketPrivado, key, command.getArchivo(), CONTENT_TYPE_PDF);

        OffsetDateTime ahora = OffsetDateTime.now();
        Contrato guardado;
        String accion;

        if (existente.isPresent()) {
            Contrato actual = existente.get();
            storagePort.deleteByUrl(actual.getArchivoPdfUrl());
            guardado = contratoRepository.save(actual.toBuilder()
                    .archivoPdfUrl(url)
                    .idUsuarioCarga(command.getIdUsuarioCarga())
                    .fechaCarga(ahora)
                    .build());
            accion = ACCION_REEMPLAZADO;
        } else {
            guardado = contratoRepository.save(Contrato.builder()
                    .idEventoPrivado(command.getIdEventoPrivado())
                    .archivoPdfUrl(url)
                    .idUsuarioCarga(command.getIdUsuarioCarga())
                    .fechaCarga(ahora)
                    .build());
            accion = ACCION_CARGADO;
        }

        registrarActividad(guardado.getId(), accion,
                "Archivo cargado para el evento #" + command.getIdEventoPrivado(),
                command.getIdUsuarioCarga());

        String accionAudit = ACCION_CARGADO.equals(accion)
                ? AuditoriaConstants.ACCION_CREAR
                : AuditoriaConstants.ACCION_ACTUALIZAR;
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioCarga(), accionAudit, AuditoriaConstants.MOD_CONTRATOS,
                "Contrato", guardado.getId(),
                null, null,
                "Contrato #" + guardado.getId() + " " + accion.toLowerCase()
                        + " para evento #" + command.getIdEventoPrivado(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        notificarContratoDisponible(guardado);

        return toQueryConDetalle(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoContratoQuery ejecutar(Long idEventoPrivado) {
        Contrato contrato = contratoRepository.findByEventoPrivado(idEventoPrivado)
                .orElseThrow(() -> new ContratoNotFoundException(
                        "No existe contrato para el evento " + idEventoPrivado));

        byte[] contenido = storagePort.downloadByUrl(contrato.getArchivoPdfUrl());

        return ArchivoContratoQuery.builder()
                .contenido(contenido)
                .nombreArchivo("contrato-evento-" + idEventoPrivado + ".pdf")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoQuery porId(Long id) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new ContratoNotFoundException(id));
        return toQueryConDetalle(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoQuery porEvento(Long idEvento) {
        Contrato contrato = contratoRepository.findByEventoPrivado(idEvento)
                .orElseThrow(() -> new ContratoNotFoundException(
                        "No existe contrato para el evento " + idEvento));
        return toQueryConDetalle(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoPageQuery ejecutar(Long idSede, LocalDate fechaEvento, Pageable pageable) {
        Page<Contrato> pagina = contratoRepository.buscarConFiltros(idSede, fechaEvento, pageable);

        return ContratoPageQuery.builder()
                .content(pagina.getContent().stream().map(this::toQuery).toList())
                .page(pagina.getNumber())
                .size(pagina.getSize())
                .totalElements(pagina.getTotalElements())
                .totalPages(pagina.getTotalPages())
                .build();
    }

    private void validarArchivo(byte[] archivo, String contentType) {
        if (archivo == null || archivo.length == 0) {
            throw new ValidationException("El archivo no puede estar vacío.");
        }
        if (archivo.length > TAMANIO_MAXIMO_BYTES) {
            throw new ValidationException("El archivo supera el tamaño máximo permitido de 15 MB.");
        }
        if (!CONTENT_TYPE_PDF.equals(contentType)) {
            throw new ValidationException("El archivo debe ser un PDF.");
        }
        if (archivo.length < FIRMA_PDF.length || !empiezaConFirmaPdf(archivo)) {
            throw new ValidationException("El archivo no es un PDF válido.");
        }
    }

    private boolean empiezaConFirmaPdf(byte[] archivo) {
        for (int i = 0; i < FIRMA_PDF.length; i++) {
            if (archivo[i] != FIRMA_PDF[i]) return false;
        }
        return true;
    }

    private void notificarContratoDisponible(Contrato contrato) {
        if (contrato.getIdCliente() == null) return;

        crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                .tipoCodigo("EVENTO_CONTRATO_LISTO")
                .destinatarioClienteId(contrato.getIdCliente())
                .entidadTipo("evento_privado")
                .entidadId(contrato.getIdEventoPrivado())
                .datosExtra(Map.of(
                        "evento", contrato.getTipoEvento() != null ? contrato.getTipoEvento() : "",
                        "fecha", contrato.getFechaEvento() != null ? contrato.getFechaEvento().toString() : ""))
                .build());
    }

    private void registrarActividad(Long idContrato, String accion, String descripcion, UUID idUsuario) {
        actividadRepository.save(ActividadContrato.builder()
                .idContrato(idContrato)
                .accion(accion)
                .descripcion(descripcion)
                .idUsuario(idUsuario)
                .build());
    }

    private ContratoQuery toQuery(Contrato c) {
        return ContratoQuery.builder()
                .id(c.getId())
                .idEventoPrivado(c.getIdEventoPrivado())
                .idCliente(c.getIdCliente())
                .archivoPdfUrl(c.getArchivoPdfUrl())
                .usuarioCarga(c.getUsuarioCarga())
                .fechaCarga(c.getFechaCarga())
                .nombreCliente(c.getNombreCliente())
                .correoCliente(c.getCorreoCliente())
                .tipoEvento(c.getTipoEvento())
                .fechaEvento(c.getFechaEvento())
                .turno(c.getTurno())
                .aforoDeclarado(c.getAforoDeclarado())
                .precioTotalContrato(c.getPrecioTotalContrato())
                .montoAdelanto(c.getMontoAdelanto())
                .saldoPendiente(c.getSaldoPendiente())
                .build();
    }

    private ContratoQuery toQueryConDetalle(Contrato c) {
        List<ActividadContratoQuery> actividades = actividadRepository
                .findByContrato(c.getId())
                .stream()
                .map(this::toActividadQuery)
                .toList();

        return toQuery(c).toBuilder()
                .actividades(actividades)
                .build();
    }

    private ActividadContratoQuery toActividadQuery(ActividadContrato a) {
        return ActividadContratoQuery.builder()
                .id(a.getId())
                .accion(a.getAccion())
                .descripcion(a.getDescripcion())
                .usuario(a.getNombreUsuario())
                .fechaAccion(a.getFechaAccion())
                .build();
    }
}
