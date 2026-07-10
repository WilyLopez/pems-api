package com.playzone.pems.application.finanzas.port.in;

import com.playzone.pems.application.finanzas.dto.command.AbrirCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.AnularMovimientoCommand;
import com.playzone.pems.application.finanzas.dto.command.CerrarCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarArqueoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarMovimientoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.ArqueoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.MovimientoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.SesionCajaQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GestionarCajaUseCase {
    SesionCajaQuery abrir(AbrirCajaCommand command);
    SesionCajaQuery cerrar(CerrarCajaCommand command);
    Optional<SesionCajaQuery> obtenerMiSesion(UUID usuarioId);
    Optional<SesionCajaQuery> obtenerMiSesionEnSede(UUID usuarioId, Long idSede);
    Optional<SesionCajaQuery> obtenerMiSesionPorFecha(UUID usuarioId, Long idSede, LocalDate fecha);
    SesionCajaQuery obtenerPorId(Long idSesionCaja);
    List<SesionCajaQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin);
    MovimientoCajaQuery registrarMovimiento(RegistrarMovimientoManualCommand command);
    List<MovimientoCajaQuery> listarMovimientos(Long idSesionCaja);
    MovimientoCajaQuery anularMovimiento(AnularMovimientoCommand command);
    ArqueoCajaQuery registrarArqueo(RegistrarArqueoCommand command);
    List<ArqueoCajaQuery> listarArqueos(Long idSesionCaja);
    ResumenCajaQuery generarResumen(Long idSesionCaja);
}
