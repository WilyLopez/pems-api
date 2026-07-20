package com.playzone.pems.application.evento.service;

import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudEventoPrivadoValidatorTest {

    @Mock private com.playzone.pems.domain.comercial.repository.TipoEventoRepository tipoEventoRepository;
    @Mock private com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository configRepository;
    @Mock private com.playzone.pems.infrastructure.security.SupabaseAuthFacade supabaseAuthFacade;
    @Mock private com.playzone.pems.domain.calendario.repository.FeriadoRepository feriadoRepository;
    @Mock private com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository bloqueRepository;
    @Mock private com.playzone.pems.domain.evento.repository.ReservaPublicaRepository reservaRepository;
    @Mock private com.playzone.pems.domain.evento.repository.EventoPrivadoRepository eventoRepository;
    @Mock private com.playzone.pems.domain.calendario.repository.TurnoRepository turnoRepository;

    private SolicitudEventoPrivadoValidator crearValidator() {
        return new SolicitudEventoPrivadoValidator(
                tipoEventoRepository, configRepository, supabaseAuthFacade,
                feriadoRepository, bloqueRepository, reservaRepository,
                eventoRepository, turnoRepository);
    }

    @Test
    void testNombreSinEdadEsRechazado() {
        SolicitudEventoPrivadoValidator validator = crearValidator();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validarNombreYEdad("Juanito", null));

        assertEquals("edadCumple", ex.getCampo());
    }

    @Test
    void testEdadSinNombreEsRechazada() {
        SolicitudEventoPrivadoValidator validator = crearValidator();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validarNombreYEdad(null, 5));

        assertEquals("nombreNino", ex.getCampo());
    }

    @Test
    void testEdadSinNombreBlancoEsRechazada() {
        SolicitudEventoPrivadoValidator validator = crearValidator();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validarNombreYEdad("   ", 5));

        assertEquals("nombreNino", ex.getCampo());
    }

    @Test
    void testNombreYEdadJuntosSonValidos() {
        SolicitudEventoPrivadoValidator validator = crearValidator();

        assertDoesNotThrow(() -> validator.validarNombreYEdad("Juanito", 5));
    }

    @Test
    void testSinNombreNiEdadEsValido() {
        SolicitudEventoPrivadoValidator validator = crearValidator();

        assertDoesNotThrow(() -> validator.validarNombreYEdad(null, null));
    }

    @Test
    void testTurnoOcupadoIndicaElCampoIdTurno() {
        SolicitudEventoPrivadoValidator validator = crearValidator();
        Turno turno = Turno.builder().id(1L).codigo("T1").build();

        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(eventoRepository.existsActivoBySedeAndFechaAndCodigoTurno(10L, LocalDate.now(), "T1"))
                .thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validarTurnoEvento(10L, LocalDate.now(), 1L));

        assertEquals("idTurno", ex.getCampo());
    }
}
