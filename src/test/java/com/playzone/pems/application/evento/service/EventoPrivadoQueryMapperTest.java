package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.query.EventoExtraQuery;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.evento.model.EventoExtra;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.model.enums.ModalidadPago;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoPrivadoQueryMapperTest {

    @Mock private EventoPrivadoRepository  eventoRepository;
    @Mock private ClientePerfilRepository  clientePerfilRepository;
    @Mock private TurnoRepository          turnoRepository;
    @Mock private EventoExtraRepository    eventoExtraRepository;
    @Mock private EventoServicioRepository eventoServicioRepository;
    @Mock private EventoCuotaRepository    cuotaRepository;
    @Mock private ExtraPaqueteRepository   extraPaqueteRepository;
    @Mock private PerfilUsuarioRepository  perfilUsuarioRepository;
    @Mock private VentaRepository          ventaRepository;
    @Mock private VentaPagoRepository      ventaPagoRepository;

    private EventoPrivadoQueryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EventoPrivadoQueryMapper(eventoRepository, clientePerfilRepository, turnoRepository,
                eventoExtraRepository, eventoServicioRepository, cuotaRepository,
                extraPaqueteRepository, perfilUsuarioRepository, ventaRepository, ventaPagoRepository);
    }

    @Test
    void testToQueryExponeCantidadYNotasDeLosExtras() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(100L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .modalidadPago(ModalidadPago.AL_CONTADO)
                .build();
        ClientePerfil cliente = ClientePerfil.builder().id(5L).nombres("Ana").correo("ana@correo.com").build();
        Turno turno = Turno.builder().id(1L).codigo("TARDE").descripcion("Tarde")
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(18, 0)).build();

        when(eventoExtraRepository.findByEvento(100L)).thenReturn(List.of(
                EventoExtra.builder().id(1L).idEventoPrivado(100L).nombreLibre("Globos")
                        .cantidad(3).notas("Color azul y blanco").build()));

        EventoPrivadoQuery query = mapper.toQuery(evento, cliente, turno, true);

        EventoExtraQuery extra = query.getExtras().get(0);
        assertEquals(3, extra.getCantidad());
        assertEquals("Color azul y blanco", extra.getNotas());
    }
}
