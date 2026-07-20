package com.playzone.pems.application.evento.service;

import com.playzone.pems.domain.calendario.exception.FechaNoDisponibleException;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.repository.TipoEventoRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.FechaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SolicitudEventoPrivadoValidator {

    private final TipoEventoRepository              tipoEventoRepository;
    private final ConfiguracionCalendarioRepository configRepository;
    private final SupabaseAuthFacade                supabaseAuthFacade;
    private final FeriadoRepository                 feriadoRepository;
    private final BloqueCalendarioRepository        bloqueRepository;
    private final ReservaPublicaRepository          reservaRepository;
    private final EventoPrivadoRepository           eventoRepository;
    private final TurnoRepository                   turnoRepository;

    public void validarTipoEvento(String tipoEventoCodigo) {
        if (tipoEventoCodigo == null || tipoEventoCodigo.isBlank()) {
            throw new ValidationException("tipoEvento", "El tipo de evento es obligatorio.");
        }
        var tipoEventoOpt = tipoEventoRepository.buscarPorCodigo(tipoEventoCodigo);
        if (tipoEventoOpt.isEmpty()) {
            throw new ValidationException("tipoEvento", "El tipo de evento especificado no existe.");
        }
        if (!tipoEventoOpt.get().isActivo()) {
            throw new ValidationException("tipoEvento", "El tipo de evento especificado no está activo.");
        }
    }

    public void validarFechaEvento(Long idSede, LocalDate fecha) {
        ConfiguracionCalendario cfg = configRepository.obtener(idSede);
        long dias = FechaUtil.diasEntre(FechaUtil.hoy(), fecha);

        boolean esAdmin = supabaseAuthFacade.tieneRol("ADMIN") || supabaseAuthFacade.tieneRol("SUPERADMIN");

        if (!esAdmin) {
            if (dias < cfg.getDiasMinEventoPrivado()) {
                throw new FechaNoDisponibleException(fecha,
                        "Los eventos privados deben reservarse con un minimo de "
                        + cfg.getDiasMinEventoPrivado()
                        + " dias de anticipacion. Por favor selecciona una fecha posterior.");
            }
            if (dias > cfg.getDiasMaxEventoPrivado()) {
                throw new ValidationException(
                        "Los eventos solo pueden agendarse hasta " + cfg.getDiasMaxEventoPrivado() + " dias adelante.");
            }
        }

        if (feriadoRepository.existsByFecha(fecha)) {
            throw new FechaNoDisponibleException(fecha, "Esta fecha es feriado.");
        }
        if (bloqueRepository.existsBloqueActivoEnFecha(idSede, fecha)) {
            throw new FechaNoDisponibleException(fecha, "La fecha esta bloqueada.");
        }
        if (reservaRepository.existsActivaBySedeAndFecha(idSede, fecha)) {
            throw new ValidationException(
                    "Esta fecha ya tiene reservas publicas y no admite eventos privados.");
        }
    }

    public void validarTurnoEvento(Long idSede, LocalDate fecha, Long idTurno) {
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ValidationException("Turno no encontrado."));
        if (eventoRepository.existsActivoBySedeAndFechaAndCodigoTurno(idSede, fecha, turno.getCodigo())) {
            throw new ValidationException("idTurno",
                    "Este turno ya tiene un evento privado. Elige otro turno u otra fecha.");
        }
    }

    public void validarNombreYEdad(String nombreNino, Integer edadCumple) {
        boolean tieneNombre = nombreNino != null && !nombreNino.isBlank();
        boolean tieneEdad = edadCumple != null;

        if (tieneNombre && !tieneEdad) {
            throw new ValidationException("edadCumple",
                    "La edad es requerida cuando se especifica el nombre del niño.");
        }
        if (tieneEdad && !tieneNombre) {
            throw new ValidationException("nombreNino",
                    "El nombre es requerido cuando se especifica la edad.");
        }
    }

    public void validarDescripcionPersonalizada(boolean esCotizacionPersonalizada, String descripcion) {
        if (!esCotizacionPersonalizada) return;
        if (descripcion == null || descripcion.trim().length() < 30) {
            throw new ValidationException("descripcionPersonalizada",
                    "La descripción debe tener al menos 30 caracteres cuando se solicita una cotización personalizada.");
        }
    }

    public void validarAforoYEdad(Long idSede, Integer aforoDeclarado, Integer edadCumple) {
        ConfiguracionCalendario cfg = configRepository.obtener(idSede);

        if (aforoDeclarado != null && aforoDeclarado > cfg.getAforoMaximo()) {
            throw new ValidationException("aforoDeclarado",
                    "El aforo declarado no puede superar " + cfg.getAforoMaximo() + " personas.");
        }
        if (edadCumple != null && (edadCumple < cfg.getEdadMinCumple() || edadCumple > cfg.getEdadMaxCumple())) {
            throw new ValidationException("edadCumple",
                    "La edad de cumple debe estar entre " + cfg.getEdadMinCumple()
                            + " y " + cfg.getEdadMaxCumple() + " años.");
        }
    }
}
