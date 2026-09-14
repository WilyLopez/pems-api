package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.dto.command.AbrirCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.AnularMovimientoCommand;
import com.playzone.pems.application.finanzas.dto.command.CerrarCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarArqueoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarMovimientoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.ArqueoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.CajaActivaQuery;
import com.playzone.pems.application.finanzas.dto.query.MovimientoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.SesionCajaQuery;
import com.playzone.pems.application.finanzas.port.in.GestionarCajaUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.finanzas.model.ArqueoCaja;
import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.ArqueoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.MovimientoCajaRepository;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.configuracion.model.ConfiguracionGlobal;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SesionCajaService implements GestionarCajaUseCase {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String CLAVE_UMBRAL_DIFERENCIA = "CAJA_UMBRAL_DIFERENCIA";
    private static final String CLAVE_MONTO_MOVIMIENTO_GRANDE = "CAJA_MONTO_MOVIMIENTO_GRANDE";
    private static final BigDecimal MONTO_MOVIMIENTO_GRANDE_DEFECTO = new BigDecimal("500");

    private final SesionCajaRepository            sesionCajaRepository;
    private final MovimientoCajaRepository        movimientoCajaRepository;
    private final ArqueoCajaRepository            arqueoCajaRepository;
    private final ConfiguracionGlobalRepository   configuracionGlobalRepository;
    private final ConfiguracionCalendarioRepository configuracionCalendarioRepository;
    private final SupabaseAuthFacade              authFacade;
    private final RegistrarLogUseCase             auditoria;
    private final CrearNotificacionPort           crearNotificacionPort;
    private final ResolverAdministradoresPort     resolverAdministradoresPort;
    private final PerfilUsuarioRepository         perfilUsuarioRepository;
    private final SedeRepository                  sedeRepository;
    private final VentaRepository                 ventaRepository;
    private final VentaPagoRepository             ventaPagoRepository;

    @Override
    public SesionCajaQuery abrir(AbrirCajaCommand command) {
        sesionCajaRepository.findAbiertaByUsuario(command.getIdUsuarioApertura())
                .ifPresent(s -> {
                    throw new ValidationException(
                            "Ya tienes una caja abierta. Cierra tu caja actual antes de abrir una nueva.");
                });

        sesionCajaRepository.findAbiertaBySede(command.getIdSede())
                .ifPresent(s -> {
                    throw new ValidationException(mensajeCajaYaAbiertaEnSede(s));
                });

        validarHorarioApertura(command.getIdSede());

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

        SesionCaja guardada;
        try {
            guardada = sesionCajaRepository.save(sesion);
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException(
                    "No se pudo abrir la caja: ya existe una sesion abierta para tu usuario o para esta sede. "
                            + "Actualiza la pagina e intenta nuevamente.");
        }
        SesionCajaQuery resultado = toQuery(guardada);
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioApertura(), AuditoriaConstants.ACCION_ABRIR, AuditoriaConstants.MOD_CAJA,
                "SesionCaja", resultado.getId(),
                null, "tipo=" + command.getTipo() + " | saldoInicial=" + saldoInicial,
                "Caja abierta (" + command.getTipo() + ") en sede #" + command.getIdSede(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        notificarAdmins("CAJA_APERTURA", Map.of(
                "sede", nombreSede(command.getIdSede()),
                "usuario", nombreUsuario(command.getIdUsuarioApertura()),
                "tipo", command.getTipo().toString(),
                "saldoInicial", saldoInicial.toPlainString()));

        return resultado;
    }

    @Override
    public SesionCajaQuery cerrar(CerrarCajaCommand command) {
        SesionCaja sesion = sesionCajaRepository.findByIdForUpdate(command.getIdSesionCaja())
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

        if (command.getSaldoFinal() == null || command.getSaldoFinal().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(
                    "El conteo fisico del efectivo (saldo contado) es obligatorio para cerrar la caja.");
        }
        BigDecimal saldoFinal    = command.getSaldoFinal();
        BigDecimal saldoEsperado = sesion.calcularSaldoEsperado();
        BigDecimal diferencia    = saldoFinal.subtract(saldoEsperado);

        BigDecimal umbral = umbralDiferencia();
        boolean sinObservaciones = command.getObservaciones() == null || command.getObservaciones().isBlank();
        if (diferencia.abs().compareTo(umbral) > 0 && sinObservaciones) {
            throw new ValidationException(
                    "La diferencia de caja (S/ " + diferencia.toPlainString()
                            + ") supera el umbral permitido. Registra una observacion que la justifique.");
        }

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

        if (diferencia.abs().compareTo(umbral) > 0) {
            notificarAdmins("CAJA_CIERRE_DISCREPANCIA", Map.of(
                    "sede", nombreSede(sesion.getIdSede()),
                    "diferencia", diferencia.toPlainString()));
        }

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

        int actualizados = command.getTipo() == TipoMovimientoCaja.INGRESO
                ? sesionCajaRepository.incrementarIngresosSiAbierta(sesion.getId(), command.getMonto())
                : sesionCajaRepository.incrementarEgresosSiAbierta(sesion.getId(), command.getMonto());
        if (actualizados == 0) {
            throw new ValidationException(
                    "La caja fue cerrada mientras se registraba el movimiento. Intenta nuevamente.");
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioRegistra(), AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_CAJA,
                "MovimientoCaja", guardado.getId(),
                null, command.getTipo().name() + "=" + command.getMonto(),
                "Movimiento manual registrado en caja #" + sesion.getId() + " | " + command.getConcepto(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        notificarSiMovimientoGrande(sesion.getIdSede(), command.getTipo().toString(),
                command.getMonto(), command.getConcepto());

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

        int actualizados = original.getTipo() == TipoMovimientoCaja.INGRESO
                ? sesionCajaRepository.incrementarIngresosSiAbierta(sesion.getId(), original.getMonto().negate())
                : sesionCajaRepository.incrementarEgresosSiAbierta(sesion.getId(), original.getMonto().negate());
        if (actualizados == 0) {
            throw new ValidationException(
                    "La caja fue cerrada mientras se anulaba el movimiento. Intenta nuevamente.");
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioAnula(), AuditoriaConstants.ACCION_ELIMINAR, AuditoriaConstants.MOD_CAJA,
                "MovimientoCaja", original.getId(),
                original.getTipo().name() + "=" + original.getMonto(), "ANULADO",
                "Movimiento #" + original.getId() + " anulado con contraasiento #" + contraasiento.getId()
                        + " | motivo=" + command.getMotivo().trim(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        notificarSiMovimientoGrande(sesion.getIdSede(), original.getTipo().toString(),
                original.getMonto(), contraasiento.getConcepto());

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

        BigDecimal umbral = umbralDiferencia();
        boolean sinObservaciones = command.getObservaciones() == null || command.getObservaciones().isBlank();
        if (diferencia.abs().compareTo(umbral) > 0 && sinObservaciones) {
            throw new ValidationException(
                    "La diferencia del arqueo (S/ " + diferencia.toPlainString()
                            + ") supera el umbral permitido. Registra una observacion que la justifique.");
        }

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

        if (diferencia.abs().compareTo(umbral) > 0) {
            notificarAdmins("CAJA_ARQUEO_DISCREPANCIA", Map.of(
                    "sede", nombreSede(sesion.getIdSede()),
                    "diferencia", diferencia.toPlainString()));
        }

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

    @Override
    @Transactional(readOnly = true)
    public Optional<CajaActivaQuery> obtenerCajaActiva(Long idSede) {
        return sesionCajaRepository.findAbiertaBySede(idSede).map(this::toCajaActivaQuery);
    }

    private CajaActivaQuery toCajaActivaQuery(SesionCaja sesion) {
        OffsetDateTime desde = sesion.getFechaApertura();
        OffsetDateTime hasta = OffsetDateTime.now(LIMA);

        List<Venta> ventas = desde != null
                ? ventaRepository.findBySedeAndFechasBetween(sesion.getIdSede(), desde, hasta, Pageable.unpaged())
                        .getContent()
                : List.of();

        BigDecimal totalVendido = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> desglosePorMedioPago = new LinkedHashMap<>();
        for (Venta venta : ventas) {
            for (VentaPago pago : ventaPagoRepository.findByVentaId(venta.getId())) {
                if (!pago.isEsValidado()) continue;
                desglosePorMedioPago.merge(pago.getMedioPagoCodigo(), pago.getMonto(), BigDecimal::add);
            }
        }

        List<MovimientoCajaQuery> movimientos = movimientoCajaRepository.findBySesion(sesion.getId())
                .stream().map(this::toMovimientoQuery).toList();
        List<ArqueoCajaQuery> arqueos = arqueoCajaRepository.findBySesion(sesion.getId())
                .stream().map(this::toArqueoQuery).toList();

        return CajaActivaQuery.builder()
                .id(sesion.getId())
                .idSede(sesion.getIdSede())
                .usuarioId(sesion.getUsuarioId())
                .nombreCajero(nombreUsuario(sesion.getUsuarioId()))
                .tipo(sesion.getTipo())
                .estado(sesion.getEstado())
                .fecha(fechaDe(sesion))
                .saldoInicial(sesion.getSaldoInicial())
                .totalIngresos(sesion.getTotalIngresos())
                .totalEgresos(sesion.getTotalEgresos())
                .saldoEsperado(sesion.getSaldoEsperado() != null
                        ? sesion.getSaldoEsperado() : sesion.calcularSaldoEsperado())
                .fechaApertura(sesion.getFechaApertura())
                .observaciones(sesion.getObservaciones())
                .cantidadVentas(ventas.size())
                .totalVendido(totalVendido)
                .desglosePorMedioPago(desglosePorMedioPago)
                .movimientos(movimientos)
                .arqueos(arqueos)
                .build();
    }

    private BigDecimal umbralDiferencia() {
        return configuracionGlobalRepository.findByClave(CLAVE_UMBRAL_DIFERENCIA)
                .map(ConfiguracionGlobal::getValor)
                .map(valor -> {
                    try {
                        return new BigDecimal(valor.trim());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal montoMovimientoGrande() {
        return configuracionGlobalRepository.findByClave(CLAVE_MONTO_MOVIMIENTO_GRANDE)
                .map(ConfiguracionGlobal::getValor)
                .map(valor -> {
                    try {
                        return new BigDecimal(valor.trim());
                    } catch (NumberFormatException e) {
                        return MONTO_MOVIMIENTO_GRANDE_DEFECTO;
                    }
                })
                .orElse(MONTO_MOVIMIENTO_GRANDE_DEFECTO);
    }

    private void notificarSiMovimientoGrande(Long idSede, String tipo, BigDecimal monto, String concepto) {
        if (monto.compareTo(montoMovimientoGrande()) < 0) return;
        notificarAdmins("CAJA_MOVIMIENTO_GRANDE", Map.of(
                "sede", nombreSede(idSede),
                "tipo", tipo,
                "monto", monto.toPlainString(),
                "concepto", concepto != null ? concepto : ""));
    }

    private void notificarAdmins(String tipoCodigo, Map<String, String> datosExtra) {
        for (UUID adminId : resolverAdministradoresPort.obtenerIdsAdministradoresActivos()) {
            crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                    .tipoCodigo(tipoCodigo)
                    .destinatarioUsuarioId(adminId)
                    .datosExtra(datosExtra)
                    .build());
        }
    }

    private String nombreSede(Long idSede) {
        return sedeRepository.findById(idSede).map(Sede::getNombre).orElse("Sede #" + idSede);
    }

    private void validarHorarioApertura(Long idSede) {
        ConfiguracionCalendario config;
        try {
            config = configuracionCalendarioRepository.obtener(idSede);
        } catch (ResourceNotFoundException e) {
            return;
        }
        if (config == null || config.getHoraApertura() == null) {
            return;
        }
        LocalTime ahora = OffsetDateTime.now(LIMA).toLocalTime();
        LocalTime limiteInferior = config.getHoraApertura().minusHours(2);
        if (ahora.isBefore(limiteInferior)) {
            throw new ValidationException(
                    "No se puede abrir la caja antes de las " + limiteInferior.format(DateTimeFormatter.ofPattern("HH:mm"))
                            + " (2 horas antes de la apertura del local).");
        }
    }

    private String mensajeCajaYaAbiertaEnSede(SesionCaja sesion) {
        String hora = sesion.getFechaApertura() != null
                ? sesion.getFechaApertura().atZoneSameInstant(LIMA)
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                : "una fecha anterior";
        return "Ya existe una caja " + sesion.getTipo() + " abierta en esta sede, a cargo de "
                + nombreUsuario(sesion.getUsuarioId()) + " desde las " + hora + ".";
    }

    private String nombreUsuario(UUID usuarioId) {
        return perfilUsuarioRepository.buscarPorId(usuarioId).map(PerfilUsuario::getNombreCompleto).orElse("");
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
