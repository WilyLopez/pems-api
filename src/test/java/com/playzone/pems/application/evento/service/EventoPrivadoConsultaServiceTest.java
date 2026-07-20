package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoPrivadoConsultaServiceTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private EventoExtraRepository eventoExtraRepository;
    @Mock private EventoServicioRepository eventoServicioRepository;
    @Mock private EventoCuotaRepository cuotaRepository;
    @Mock private ExtraPaqueteRepository extraPaqueteRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private SedeScopeValidator sedeScope;
    @Mock private SupabaseAuthFacade supabaseAuthFacade;

    private EventoPrivadoConsultaService service;

    @BeforeEach
    void setUp() {
        EventoPrivadoQueryMapper mapper = new EventoPrivadoQueryMapper(
                eventoRepository, clientePerfilRepository, turnoRepository,
                eventoExtraRepository, eventoServicioRepository, cuotaRepository,
                extraPaqueteRepository, perfilUsuarioRepository, ventaRepository, ventaPagoRepository);
        EventoAccesoValidator accesoValidator = new EventoAccesoValidator(supabaseAuthFacade, sedeScope);
        service = new EventoPrivadoConsultaService(eventoRepository, sedeScope, accesoValidator, mapper);
    }

    private ClientePerfil clientePrueba() {
        return ClientePerfil.builder().id(5L).nombres("Ana").correo("ana@correo.com").build();
    }

    private Turno turnoPrueba() {
        return Turno.builder().id(1L).codigo("TARDE").descripcion("Tarde")
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(18, 0)).build();
    }

    @Test
    void testConsultarPorIdComoClienteDuenoDelEventoNoLanzaExcepcion() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(800L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .build();
        when(eventoRepository.findById(800L)).thenReturn(Optional.of(evento));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(5L));

        EventoPrivadoQuery resultado = service.consultarPorId(800L);

        assertEquals(800L, resultado.getId());
        verify(sedeScope, never()).validarAcceso(anyLong());
    }

    @Test
    void testConsultarPorIdComoClienteAjenoAlEventoLanzaAccessDeniedException() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(801L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .build();
        when(eventoRepository.findById(801L)).thenReturn(Optional.of(evento));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(9L));

        assertThrows(AccessDeniedException.class, () -> service.consultarPorId(801L));
    }
}
