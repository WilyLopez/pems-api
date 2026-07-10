package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.dto.command.AnularIngresoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarIngresoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.RegistroIngresoQuery;
import com.playzone.pems.application.finanzas.port.in.RegistrarIngresoUseCase;
import com.playzone.pems.domain.finanzas.model.RegistroIngreso;
import com.playzone.pems.domain.finanzas.model.TipoIngreso;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.RegistroIngresoRepository;
import com.playzone.pems.domain.finanzas.repository.TipoIngresoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IngresoService implements RegistrarIngresoUseCase {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final RegistroIngresoRepository registroIngresoRepository;
    private final TipoIngresoRepository     tipoIngresoRepository;
    private final EnrutadorCajaService      enrutadorCajaService;
    private final RegistrarLogUseCase       auditoria;
    private final SedeScopeValidator        sedeScope;

    @Override
    public RegistroIngresoQuery registrar(RegistrarIngresoManualCommand command) {
        TipoIngreso tipo = tipoIngresoRepository.findById(command.getTipoIngresoCodigo())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ingreso no encontrado."));
        if (!tipo.isActivo()) {
            throw new ValidationException("El tipo de ingreso seleccionado no está activo.");
        }
        RegistroIngreso ingreso = RegistroIngreso.builder()
                .tipoIngresoCodigo(command.getTipoIngresoCodigo())
                .idSede(command.getIdSede())
                .monto(command.getMonto())
                .fecha(command.getFecha())
                .fechaCobro(LocalDate.now(LIMA))
                .medioPago(command.getMedioPago())
                .descripcion(command.getDescripcion())
                .esAutomatico(false)
                .idUsuarioRegistra(command.getIdUsuarioRegistra())
                .build();
        RegistroIngresoQuery resultado = toQuery(registroIngresoRepository.save(ingreso));
        enrutadorCajaService.registrarIngresoManualEfectivo(
                command.getIdUsuarioRegistra(), command.getMedioPago(), command.getMonto(),
                "Ingreso manual " + tipo.getNombre() + " #" + resultado.getId(), resultado.getId());
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioRegistra(), AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_FINANZAS,
                "RegistroIngreso", resultado.getId(),
                null, "monto=" + command.getMonto() + " | tipo=" + command.getTipoIngresoCodigo(),
                "Ingreso manual registrado: " + command.getTipoIngresoCodigo() + " | S/ " + command.getMonto(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroIngresoQuery> listar(Long idSede, Pageable pageable) {
        return registroIngresoRepository.findBySede(idSede, pageable).map(this::toQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroIngresoQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return registroIngresoRepository.findBySedeAndRangoFecha(idSede, inicio, fin)
                .stream().map(this::toQuery).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroIngresoQuery> listarTesoreriaWeb(Long idSede, LocalDate inicio, LocalDate fin) {
        return registroIngresoRepository.findTesoreriaWeb(idSede, inicio, fin)
                .stream().map(this::toQuery).toList();
    }

    @Override
    public RegistroIngresoQuery anular(AnularIngresoCommand command) {
        if (command.getMotivo() == null || command.getMotivo().isBlank()) {
            throw new ValidationException("El motivo de anulacion es obligatorio.");
        }
        RegistroIngreso original = registroIngresoRepository.findById(command.getIdIngreso())
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado."));
        sedeScope.validarAcceso(original.getIdSede());
        if (original.esContraasiento()) {
            throw new ValidationException("Un contraasiento no puede anularse.");
        }
        if (registroIngresoRepository.existsAnuladoPara(original.getId())) {
            throw new ValidationException("El ingreso ya fue anulado.");
        }

        RegistroIngreso contraasiento = RegistroIngreso.builder()
                .tipoIngresoCodigo(original.getTipoIngresoCodigo())
                .idSede(original.getIdSede())
                .idReservaPublica(original.getIdReservaPublica())
                .idEventoPrivado(original.getIdEventoPrivado())
                .monto(original.getMonto())
                .fecha(original.getFecha())
                .fechaCobro(original.getFechaCobro())
                .medioPago(original.getMedioPago())
                .descripcion("Anulacion ingreso #" + original.getId() + " | " + command.getMotivo().trim())
                .esAutomatico(false)
                .naturaleza(NaturalezaMovimientoCaja.CONTRAASIENTO)
                .idRegistroAnulado(original.getId())
                .idUsuarioRegistra(command.getIdUsuarioAnula())
                .build();
        RegistroIngresoQuery resultado;
        try {
            resultado = toQuery(registroIngresoRepository.save(contraasiento));
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("El ingreso ya fue anulado.");
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioAnula(), AuditoriaConstants.ACCION_ANULAR, AuditoriaConstants.MOD_FINANZAS,
                "RegistroIngreso", original.getId(),
                "monto=" + original.getMonto(), "ANULADO",
                "Ingreso #" + original.getId() + " anulado con contraasiento #" + resultado.getId()
                        + " | motivo=" + command.getMotivo().trim(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    private RegistroIngresoQuery toQuery(RegistroIngreso r) {
        return RegistroIngresoQuery.builder()
                .id(r.getId())
                .tipoIngresoCodigo(r.getTipoIngresoCodigo())
                .idSede(r.getIdSede())
                .idReservaPublica(r.getIdReservaPublica())
                .idEventoPrivado(r.getIdEventoPrivado())
                .monto(r.getMonto())
                .fecha(r.getFecha())
                .fechaCobro(r.getFechaCobro())
                .medioPago(r.getMedioPago())
                .descripcion(r.getDescripcion())
                .esAutomatico(r.isEsAutomatico())
                .naturaleza(r.getNaturaleza())
                .idRegistroAnulado(r.getIdRegistroAnulado())
                .fechaCreacion(r.getFechaCreacion())
                .build();
    }
}
