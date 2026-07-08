package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.dto.command.AbrirCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.AnularMovimientoCommand;
import com.playzone.pems.application.finanzas.dto.command.CerrarCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarArqueoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarMovimientoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.ArqueoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.MovimientoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.SesionCajaQuery;
import com.playzone.pems.application.finanzas.port.in.GestionarCajaUseCase;
import com.playzone.pems.domain.finanzas.model.ArqueoCaja;
import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.ArqueoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.MovimientoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SesionCajaService implements GestionarCajaUseCase {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final SesionCajaRepository      sesionCajaRepository;
    private final MovimientoCajaRepository  movimientoCajaRepository;
    private final ArqueoCajaRepository      arqueoCajaRepository;
    private final SupabaseAuthFacade        authFacade;
    private final RegistrarLogUseCase       auditoria;

    @Override
    public SesionCajaQuery abrir(AbrirCajaCommand command) {
        sesionCajaRepository.findAbiertaByUsuario(command.getIdUsuarioApertura())
                .ifPresent(s -> {
                    throw new ValidationException(
                            "Ya tienes una caja abierta. Cierra tu caja actual antes de abrir una nueva.");
                });

        BigDecimal saldoInicial = command.getSaldoInicial() != null
                ? command.getSaldoInicial() : BigDecimal.ZERO;
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El saldo inicial no puede ser negativo.");
        }

        SesionCaja sesion = SesionCaja.builder()
                .idSede(command.getIdSede())
                .usuarioId(command.getIdUsuarioApertura())
                .tipo(command.getTipo())
                .estado(EstadoCaja.ABIERTA)
                .saldoInicial(saldoInicial)
                .totalIngresos(BigDecimal.ZERO)
                .totalEgresos(BigDecimal.ZERO)
                .fechaApertura(OffsetDateTime.now(LIMA))
                .observaciones(command.getObservaciones())
                .build();

        SesionCajaQuery resultado = toQuery(sesionCajaRepository.save(sesion));
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioApertura(), AuditoriaConstants.ACCION_ABRIR, AuditoriaConstants.MOD_CAJA,
                "SesionCaja", resultado.getId(),
                null, "tipo=" + command.getTipo() + " | saldoInicial=" + saldoInicial,
                "Caja abierta (" + command.getTipo() + ") en sede #" + command.getIdSede(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public SesionCajaQuery cerrar(CerrarCajaCommand command) {
        SesionCaja sesion = sesionCajaRepository.findById(command.getIdSesionCaja())
                .orElseThrow(() -> new ResourceNotFoundException("Sesion de caja no encontrada."));
        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ValidationException("La caja ya está cerrada.");
        }

        boolean esPropia = sesion.getUsuarioId().equals(command.getIdUsuarioCierre());
        if (!esPropia && !command.isEsAdmin()) {
            throw new ValidationException("No puedes cerrar la caja de otro usuario.");
        }
        String motivo = command.getMotivo();
        if (!esPropia) {
            if (motivo == null || motivo.isBlank()) {
                throw new ValidationException(
                        "El cierre de una caja ajena requiere un motivo obligatorio.");
            }
        }

        BigDecimal saldoFinal    = command.getSaldoFinal() != null ? command.getSaldoFinal() : BigDecimal.ZERO;
        BigDecimal saldoEsperado = sesion.calcularSaldoEsperado();
        BigDecimal diferencia    = saldoFinal.subtract(saldoEsperado);

        SesionCaja cerrada = sesion.toBuilder()
                .estado(EstadoCaja.CERRADA)
                .saldoFinal(saldoFinal)
                .saldoEsperado(saldoEsperado)
                .diferencia(diferencia)
                .fechaCierre(OffsetDateTime.now(LIMA))
                .cerradaPor(command.getIdUsuarioCierre())
                .motivoCierre(motivo)
                .observaciones(command.getObservaciones() != null
                        ? command.getObservaciones() : sesion.getObservaciones())
                .build();

        SesionCajaQuery resultado = toQuery(sesionCajaRepository.save(cerrada));
        String detalle = esPropia
                ? "Caja #" + sesion.getId() + " cerrada por su titular"
                : "Caja #" + sesion.getId() + " cerrada por administrador | motivo=" + motivo;
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioCierre(), AuditoriaConstants.ACCION_CERRAR, AuditoriaConstants.MOD_CAJA,
                "SesionCaja", sesion.getId(),
                "ABIERTA", "CERRADA",
                detalle + " | saldoFinal=" + saldoFinal + " | diferencia=" + diferencia,
                null, null,
                esPropia ? AuditoriaConstants.NIVEL_INFO : AuditoriaConstants.NIVEL_WARNING,
                AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCajaQuery> obtenerMiSesion(UUID usuarioId) {
        return sesionCajaRepository.findAbiertaByUsuario(usuarioId).map(this::toQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCajaQuery> obtenerMiSesionEnSede(UUID usuarioId, Long idSede) {
        return sesionCajaRepository.findAbiertaByUsuarioAndSede(usuarioId, idSede).map(this::toQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCajaQuery> obtenerMiSesionPorFecha(UUID usuarioId, Long idSede, LocalDate fecha) {
        return sesionCajaRepository.findByUsuarioAndSedeAndFecha(usuarioId, idSede, fecha).map(this::toQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaQuery obtenerPorId(Long idSesionCaja) {
        return sesionCajaRepository.findById(idSesionCaja)
                .map(this::toQuery)
                .orElseThrow(() -> new ResourceNotFoundException("Sesion de caja no encontrada."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCajaQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return sesionCajaRepository.findBySedeAndRango(idSede, inicio, fin)
                .stream().map(this::toQuery).toList();
    }

    @Override
    public MovimientoCajaQuery registrarMovimiento(RegistrarMovimientoManualCommand command) {
        SesionCaja sesion = obtenerSesionAccesible(command.getIdSesionCaja());
        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ValidationException("No se puede registrar movimientos en una caja cerrada.");
        }
        if (command.getMonto() == null || command.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El monto del movimiento debe ser mayor a 0.");
        }

        MovimientoCaja movimiento = MovimientoCaja.builder()
                .idSesionCaja(command.getIdSesionCaja())
                .tipo(command.getTipo())
                .concepto(command.getConcepto())
                .monto(command.getMonto())
                .medioPago(command.getMedioPago())
                .categoriaRetiro(command.getCategoriaRetiro())
                .esManual(true)
                .idUsuarioRegistra(command.getIdUsuarioRegistra())
                .build();
        MovimientoCaja guardado = movimientoCajaRepository.save(movimiento);

        if (command.getTipo() == TipoMovimientoCaja.INGRESO) {
            sesionCajaRepository.incrementarIngresos(sesion.getId(), command.getMonto());
        } else {
            sesionCajaRepository.incrementarEgresos(sesion.getId(), command.getMonto());
        }

        return toMovimientoQuery(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoCajaQuery> listarMovimientos(Long idSesionCaja) {
        obtenerSesionAccesible(idSesionCaja);
        return movimientoCajaRepository.findBySesion(idSesionCaja)
                .stream().map(this::toMovimientoQuery).toList();
    }

    @Override
    public MovimientoCajaQuery anularMovimiento(AnularMovimientoCommand command) {
        if (command.getMotivo() == null || command.getMotivo().isBlank()) {
            throw new ValidationException("El motivo de anulacion es obligatorio.");
        }
        MovimientoCaja original = movimientoCajaRepository.findById(command.getIdMovimiento())
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de caja no encontrado."));
        SesionCaja sesion = obtenerSesionAccesible(original.getIdSesionCaja());
        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ValidationException("No se pueden anular movimientos de una caja cerrada.");
        }
        if (original.esContraasiento()) {
            throw new ValidationException("Un contraasiento no puede anularse.");
        }
        if (movimientoCajaRepository.existsContraasientoPara(original.getId())) {
            throw new ValidationException("El movimiento ya fue anulado.");
        }

        MovimientoCaja contraasiento = movimientoCajaRepository.save(MovimientoCaja.builder()
                .idSesionCaja(sesion.getId())
                .tipo(original.getTipo())
                .concepto("Anulacion movimiento #" + original.getId() + " | " + command.getMotivo().trim())
                .monto(original.getMonto())
                .medioPago(original.getMedioPago())
                .esManual(true)
                .naturaleza(NaturalezaMovimientoCaja.CONTRAASIENTO)
                .idMovimientoAnulado(original.getId())
                .idUsuarioRegistra(command.getIdUsuarioAnula())
                .build());

        if (original.getTipo() == TipoMovimientoCaja.INGRESO) {
            sesionCajaRepository.incrementarIngresos(sesion.getId(), original.getMonto().negate());
        } else {
            sesionCajaRepository.incrementarEgresos(sesion.getId(), original.getMonto().negate());
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioAnula(), AuditoriaConstants.ACCION_ELIMINAR, AuditoriaConstants.MOD_CAJA,
                "MovimientoCaja", original.getId(),
                original.getTipo().name() + "=" + original.getMonto(), "ANULADO",
                "Movimiento #" + original.getId() + " anulado con contraasiento #" + contraasiento.getId()
                        + " | motivo=" + command.getMotivo().trim(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        return toMovimientoQuery(contraasiento);
    }

    @Override
    public ArqueoCajaQuery registrarArqueo(RegistrarArqueoCommand command) {
        SesionCaja sesion = obtenerSesionAccesible(command.getIdSesionCaja());
        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ValidationException("No se puede registrar un arqueo en una caja cerrada.");
        }

        BigDecimal saldoEsperado = sesion.calcularSaldoEsperado();
        BigDecimal diferencia    = command.getSaldoContado().subtract(saldoEsperado);

        ArqueoCaja arqueo = ArqueoCaja.builder()
                .idSesionCaja(command.getIdSesionCaja())
                .saldoEsperado(saldoEsperado)
                .saldoContado(command.getSaldoContado())
                .diferencia(diferencia)
                .observaciones(command.getObservaciones())
                .realizadoPor(command.getRealizadoPor())
                .build();
        ArqueoCajaQuery resultado = toArqueoQuery(arqueoCajaRepository.save(arqueo));
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getRealizadoPor(), AuditoriaConstants.ACCION_ARQUEO, AuditoriaConstants.MOD_CAJA,
                "ArqueoCaja", resultado.getId(),
                null, "contado=" + command.getSaldoContado() + " | diferencia=" + diferencia,
                "Arqueo en caja #" + command.getIdSesionCaja() + " | diferencia=" + diferencia,
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArqueoCajaQuery> listarArqueos(Long idSesionCaja) {
        obtenerSesionAccesible(idSesionCaja);
        return arqueoCajaRepository.findBySesion(idSesionCaja)
                .stream().map(this::toArqueoQuery).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCajaQuery generarResumen(Long idSesionCaja) {
        SesionCaja sesion = obtenerSesionAccesible(idSesionCaja);

        List<MovimientoCajaQuery> movimientos = movimientoCajaRepository.findBySesion(idSesionCaja)
                .stream().map(this::toMovimientoQuery).toList();

        List<ArqueoCajaQuery> arqueos = arqueoCajaRepository.findBySesion(idSesionCaja)
                .stream().map(this::toArqueoQuery).toList();

        return ResumenCajaQuery.builder()
                .id(sesion.getId())
                .idSede(sesion.getIdSede())
                .usuarioId(sesion.getUsuarioId())
                .tipo(sesion.getTipo())
                .fecha(fechaDe(sesion))
                .saldoInicial(sesion.getSaldoInicial())
                .totalIngresos(sesion.getTotalIngresos())
                .totalEgresos(sesion.getTotalEgresos())
                .saldoEsperado(sesion.getSaldoEsperado() != null
                        ? sesion.getSaldoEsperado() : sesion.calcularSaldoEsperado())
                .saldoFinal(sesion.getSaldoFinal())
                .diferencia(sesion.getDiferencia())
                .estado(sesion.getEstado())
                .fechaApertura(sesion.getFechaApertura())
                .fechaCierre(sesion.getFechaCierre())
                .observaciones(sesion.getObservaciones())
                .movimientos(movimientos)
                .arqueos(arqueos)
                .build();
    }

    private SesionCaja obtenerSesionAccesible(Long idSesionCaja) {
        SesionCaja sesion = sesionCajaRepository.findById(idSesionCaja)
                .orElseThrow(() -> new ResourceNotFoundException("Sesion de caja no encontrada."));
        boolean esAdmin = authFacade.tieneRol("SUPERADMIN") || authFacade.tieneRol("ADMIN");
        if (esAdmin) {
            return sesion;
        }
        UUID usuarioActual = authFacade.usuarioActualId().orElse(null);
        if (usuarioActual == null || !usuarioActual.equals(sesion.getUsuarioId())) {
            throw new AccessDeniedException("No tienes acceso a esta sesion de caja.");
        }
        return sesion;
    }

    private LocalDate fechaDe(SesionCaja s) {
        return s.getFechaApertura() != null
                ? s.getFechaApertura().atZoneSameInstant(LIMA).toLocalDate()
                : null;
    }

    private SesionCajaQuery toQuery(SesionCaja s) {
        return SesionCajaQuery.builder()
                .id(s.getId())
                .idSede(s.getIdSede())
                .usuarioId(s.getUsuarioId())
                .tipo(s.getTipo())
                .fecha(fechaDe(s))
                .saldoInicial(s.getSaldoInicial())
                .saldoFinal(s.getSaldoFinal())
                .totalIngresos(s.getTotalIngresos())
                .totalEgresos(s.getTotalEgresos())
                .saldoEsperado(s.getSaldoEsperado())
                .diferencia(s.getDiferencia())
                .estado(s.getEstado())
                .cerradaPor(s.getCerradaPor())
                .motivoCierre(s.getMotivoCierre())
                .fechaApertura(s.getFechaApertura())
                .fechaCierre(s.getFechaCierre())
                .observaciones(s.getObservaciones())
                .build();
    }

    private MovimientoCajaQuery toMovimientoQuery(MovimientoCaja m) {
        return MovimientoCajaQuery.builder()
                .id(m.getId())
                .idSesionCaja(m.getIdSesionCaja())
                .tipo(m.getTipo())
                .concepto(m.getConcepto())
                .monto(m.getMonto())
                .medioPago(m.getMedioPago())
                .categoriaRetiro(m.getCategoriaRetiro())
                .idRegistroIngreso(m.getIdRegistroIngreso())
                .idRegistroEgreso(m.getIdRegistroEgreso())
                .idVenta(m.getIdVenta())
                .esManual(m.isEsManual())
                .naturaleza(m.getNaturaleza())
                .idMovimientoAnulado(m.getIdMovimientoAnulado())
                .fechaCreacion(m.getFechaCreacion())
                .build();
    }

    private ArqueoCajaQuery toArqueoQuery(ArqueoCaja a) {
        return ArqueoCajaQuery.builder()
                .id(a.getId())
                .idSesionCaja(a.getIdSesionCaja())
                .saldoEsperado(a.getSaldoEsperado())
                .saldoContado(a.getSaldoContado())
                .diferencia(a.getDiferencia())
                .observaciones(a.getObservaciones())
                .realizadoPor(a.getRealizadoPor())
                .fechaCreacion(a.getFechaCreacion())
                .build();
    }
}
