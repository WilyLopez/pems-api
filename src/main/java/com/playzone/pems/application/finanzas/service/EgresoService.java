package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.dto.command.AnularEgresoCommand;
import com.playzone.pems.application.finanzas.dto.command.AprobarEgresoCommand;
import com.playzone.pems.application.finanzas.dto.command.RechazarEgresoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarEgresoCommand;
import com.playzone.pems.application.finanzas.dto.query.RegistroEgresoQuery;
import com.playzone.pems.application.finanzas.port.in.RegistrarEgresoUseCase;
import com.playzone.pems.domain.configuracion.model.ConfiguracionGlobal;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.finanzas.model.RegistroEgreso;
import com.playzone.pems.domain.finanzas.model.TipoEgreso;
import com.playzone.pems.domain.finanzas.model.enums.EstadoAprobacionEgreso;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.RegistroEgresoRepository;
import com.playzone.pems.domain.finanzas.repository.TipoEgresoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class EgresoService implements RegistrarEgresoUseCase {

    private static final String CLAVE_UMBRAL_APROBACION = "EGRESO_UMBRAL_APROBACION";

    private final RegistroEgresoRepository       registroEgresoRepository;
    private final TipoEgresoRepository           tipoEgresoRepository;
    private final ConfiguracionGlobalRepository  configuracionGlobalRepository;
    private final EnrutadorCajaService           enrutadorCajaService;
    private final RegistrarLogUseCase            auditoria;
    private final SedeScopeValidator             sedeScope;

    @Override
    public RegistroEgresoQuery registrar(RegistrarEgresoCommand command) {
        TipoEgreso tipo = tipoEgresoRepository.findById(command.getTipoEgresoCodigo())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de egreso no encontrado."));
        if (!tipo.isActivo()) {
            throw new ValidationException("El tipo de egreso seleccionado no está activo.");
        }

        boolean requiereAprobacion = command.getMonto().compareTo(umbralAprobacion()) >= 0;

        RegistroEgreso egreso = RegistroEgreso.builder()
                .tipoEgresoCodigo(command.getTipoEgresoCodigo())
                .idSede(command.getIdSede())
                .monto(command.getMonto())
                .fecha(command.getFecha())
                .medioPago(command.getMedioPago())
                .periodoAnio(command.getPeriodoAnio())
                .periodoMes(command.getPeriodoMes())
                .descripcion(command.getDescripcion())
                .comprobanteUrl(command.getComprobanteUrl())
                .esRecurrente(command.isEsRecurrente())
                .estadoAprobacion(requiereAprobacion
                        ? EstadoAprobacionEgreso.PENDIENTE_APROBACION
                        : EstadoAprobacionEgreso.APROBADO)
                .idUsuarioRegistra(command.getIdUsuarioRegistra())
                .build();
        RegistroEgresoQuery resultado = toQuery(registroEgresoRepository.save(egreso));

        if (requiereAprobacion) {
            auditoria.ejecutar(new RegistrarLogUseCase.Command(
                    command.getIdUsuarioRegistra(), AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_FINANZAS,
                    "RegistroEgreso", resultado.getId(),
                    null, "monto=" + command.getMonto() + " | tipo=" + command.getTipoEgresoCodigo(),
                    "Egreso registrado, pendiente de aprobacion (umbral S/ " + umbralAprobacion() + "): "
                            + command.getTipoEgresoCodigo() + " | S/ " + command.getMonto(),
                    null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));
            return resultado;
        }

        enrutadorCajaService.registrarEgresoManualEfectivo(
                command.getIdUsuarioRegistra(), command.getMedioPago(), command.getMonto(),
                "Egreso " + tipo.getNombre() + " #" + resultado.getId(), resultado.getId());
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioRegistra(), AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_FINANZAS,
                "RegistroEgreso", resultado.getId(),
                null, "monto=" + command.getMonto() + " | tipo=" + command.getTipoEgresoCodigo(),
                "Egreso registrado: " + command.getTipoEgresoCodigo() + " | S/ " + command.getMonto(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroEgresoQuery> listar(Long idSede, Pageable pageable) {
        return registroEgresoRepository.findBySede(idSede, pageable).map(this::toQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroEgresoQuery> listarPorPeriodo(Long idSede, int anio, int mes) {
        return registroEgresoRepository.findBySedeAndPeriodo(idSede, anio, mes).stream()
                .map(this::toQuery)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroEgresoQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return registroEgresoRepository.findBySedeAndRangoFecha(idSede, inicio, fin).stream()
                .map(this::toQuery)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroEgresoQuery> listarPendientesAprobacion(Long idSede) {
        return registroEgresoRepository.findPendientesAprobacion(idSede).stream()
                .map(this::toQuery)
                .toList();
    }

    @Override
    public RegistroEgresoQuery aprobar(AprobarEgresoCommand command) {
        if (!command.isEsAdmin()) {
            throw new AccessDeniedException("Solo un administrador puede aprobar egresos.");
        }
        RegistroEgreso egreso = registroEgresoRepository.findByIdForUpdate(command.getIdEgreso())
                .orElseThrow(() -> new ResourceNotFoundException("Egreso no encontrado."));
        sedeScope.validarAcceso(egreso.getIdSede());
        if (!egreso.estaPendienteAprobacion()) {
            throw new ValidationException("El egreso no esta pendiente de aprobacion.");
        }
        if (Objects.equals(egreso.getIdUsuarioRegistra(), command.getIdUsuarioAprueba())) {
            throw new ValidationException("Quien registro el egreso no puede aprobarlo.");
        }

        RegistroEgreso aprobado = egreso.toBuilder()
                .estadoAprobacion(EstadoAprobacionEgreso.APROBADO)
                .aprobadoPor(command.getIdUsuarioAprueba())
                .fechaAprobacion(OffsetDateTime.now())
                .build();
        RegistroEgresoQuery resultado = toQuery(registroEgresoRepository.save(aprobado));

        TipoEgreso tipo = tipoEgresoRepository.findById(egreso.getTipoEgresoCodigo()).orElse(null);
        String nombreTipo = tipo != null ? tipo.getNombre() : egreso.getTipoEgresoCodigo();
        enrutadorCajaService.registrarEgresoManualEfectivo(
                command.getIdUsuarioAprueba(), egreso.getMedioPago(), egreso.getMonto(),
                "Egreso " + nombreTipo + " #" + egreso.getId() + " (aprobado)", egreso.getId());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioAprueba(), AuditoriaConstants.ACCION_APROBAR, AuditoriaConstants.MOD_FINANZAS,
                "RegistroEgreso", egreso.getId(),
                "PENDIENTE_APROBACION", "APROBADO",
                "Egreso #" + egreso.getId() + " aprobado | monto=" + egreso.getMonto(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public RegistroEgresoQuery rechazar(RechazarEgresoCommand command) {
        if (!command.isEsAdmin()) {
            throw new AccessDeniedException("Solo un administrador puede rechazar egresos.");
        }
        if (command.getMotivo() == null || command.getMotivo().isBlank()) {
            throw new ValidationException("El motivo de rechazo es obligatorio.");
        }
        RegistroEgreso egreso = registroEgresoRepository.findByIdForUpdate(command.getIdEgreso())
                .orElseThrow(() -> new ResourceNotFoundException("Egreso no encontrado."));
        sedeScope.validarAcceso(egreso.getIdSede());
        if (!egreso.estaPendienteAprobacion()) {
            throw new ValidationException("El egreso no esta pendiente de aprobacion.");
        }
        if (Objects.equals(egreso.getIdUsuarioRegistra(), command.getIdUsuarioAprueba())) {
            throw new ValidationException("Quien registro el egreso no puede rechazarlo.");
        }

        RegistroEgreso rechazado = egreso.toBuilder()
                .estadoAprobacion(EstadoAprobacionEgreso.RECHAZADO)
                .aprobadoPor(command.getIdUsuarioAprueba())
                .fechaAprobacion(OffsetDateTime.now())
                .motivoRechazo(command.getMotivo().trim())
                .build();
        RegistroEgresoQuery resultado = toQuery(registroEgresoRepository.save(rechazado));

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioAprueba(), AuditoriaConstants.ACCION_RECHAZAR, AuditoriaConstants.MOD_FINANZAS,
                "RegistroEgreso", egreso.getId(),
                "PENDIENTE_APROBACION", "RECHAZADO",
                "Egreso #" + egreso.getId() + " rechazado | motivo=" + command.getMotivo().trim(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public RegistroEgresoQuery anular(AnularEgresoCommand command) {
        if (command.getMotivo() == null || command.getMotivo().isBlank()) {
            throw new ValidationException("El motivo de anulacion es obligatorio.");
        }
        RegistroEgreso original = registroEgresoRepository.findById(command.getIdEgreso())
                .orElseThrow(() -> new ResourceNotFoundException("Egreso no encontrado."));
        sedeScope.validarAcceso(original.getIdSede());
        if (original.esContraasiento()) {
            throw new ValidationException("Un contraasiento no puede anularse.");
        }
        if (original.getEstadoAprobacion() != EstadoAprobacionEgreso.APROBADO) {
            throw new ValidationException("Solo se puede anular un egreso aprobado.");
        }
        if (registroEgresoRepository.existsAnuladoPara(original.getId())) {
            throw new ValidationException("El egreso ya fue anulado.");
        }

        RegistroEgreso contraasiento = RegistroEgreso.builder()
                .tipoEgresoCodigo(original.getTipoEgresoCodigo())
                .idSede(original.getIdSede())
                .monto(original.getMonto())
                .fecha(original.getFecha())
                .medioPago(original.getMedioPago())
                .periodoAnio(original.getPeriodoAnio())
                .periodoMes(original.getPeriodoMes())
                .descripcion("Anulacion egreso #" + original.getId() + " | " + command.getMotivo().trim())
                .esRecurrente(false)
                .naturaleza(NaturalezaMovimientoCaja.CONTRAASIENTO)
                .idRegistroAnulado(original.getId())
                .idUsuarioRegistra(command.getIdUsuarioAnula())
                .build();
        RegistroEgresoQuery resultado;
        try {
            resultado = toQuery(registroEgresoRepository.save(contraasiento));
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("El egreso ya fue anulado.");
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioAnula(), AuditoriaConstants.ACCION_ANULAR, AuditoriaConstants.MOD_FINANZAS,
                "RegistroEgreso", original.getId(),
                "monto=" + original.getMonto(), "ANULADO",
                "Egreso #" + original.getId() + " anulado con contraasiento #" + resultado.getId()
                        + " | motivo=" + command.getMotivo().trim(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    private BigDecimal umbralAprobacion() {
        return configuracionGlobalRepository.findByClave(CLAVE_UMBRAL_APROBACION)
                .map(ConfiguracionGlobal::getValor)
                .map(valor -> {
                    try {
                        return new BigDecimal(valor.trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(new BigDecimal("500"));
    }

    private RegistroEgresoQuery toQuery(RegistroEgreso r) {
        return RegistroEgresoQuery.builder()
                .id(r.getId())
                .tipoEgresoCodigo(r.getTipoEgresoCodigo())
                .idSede(r.getIdSede())
                .monto(r.getMonto())
                .fecha(r.getFecha())
                .medioPago(r.getMedioPago())
                .periodoAnio(r.getPeriodoAnio())
                .periodoMes(r.getPeriodoMes())
                .descripcion(r.getDescripcion())
                .comprobanteUrl(r.getComprobanteUrl())
                .esRecurrente(r.isEsRecurrente())
                .naturaleza(r.getNaturaleza())
                .idRegistroAnulado(r.getIdRegistroAnulado())
                .estadoAprobacion(r.getEstadoAprobacion())
                .aprobadoPor(r.getAprobadoPor())
                .fechaAprobacion(r.getFechaAprobacion())
                .motivoRechazo(r.getMotivoRechazo())
                .idUsuarioRegistra(r.getIdUsuarioRegistra())
                .fechaCreacion(r.getFechaCreacion())
                .build();
    }
}
