package com.playzone.pems.interfaces.rest.contrato;

import com.playzone.pems.application.contrato.dto.query.ArchivoContratoQuery;
import com.playzone.pems.application.contrato.dto.query.ContratoQuery;
import com.playzone.pems.application.contrato.port.in.CargarContratoUseCase;
import com.playzone.pems.application.contrato.port.in.DescargarContratoUseCase;
import com.playzone.pems.application.contrato.port.in.ListarContratosUseCase;
import com.playzone.pems.application.contrato.port.in.ObtenerContratoUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.contrato.mapper.ContratoResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoControllerTest {

    @Mock private CargarContratoUseCase    cargarUseCase;
    @Mock private DescargarContratoUseCase descargarUseCase;
    @Mock private ObtenerContratoUseCase   obtenerUseCase;
    @Mock private ListarContratosUseCase   listarUseCase;
    @Mock private ContratoResponseMapper   mapper;
    @Mock private SupabaseAuthFacade       supabaseAuthFacade;

    private ContratoController controller;

    @BeforeEach
    void setUp() {
        controller = new ContratoController(
                cargarUseCase, descargarUseCase, obtenerUseCase, listarUseCase,
                mapper, supabaseAuthFacade);
    }

    private ContratoQuery queryDeCliente(Long idCliente) {
        return ContratoQuery.builder().id(1L).idEventoPrivado(4L).idCliente(idCliente).build();
    }

    @Test
    void testObtenerPorEventoPermiteAlClienteDueno() {
        when(obtenerUseCase.porEvento(4L)).thenReturn(queryDeCliente(7L));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(7L));

        ResponseEntity<?> respuesta = controller.obtenerPorEvento(4L);

        assertEquals(200, respuesta.getStatusCode().value());
    }

    @Test
    void testObtenerPorEventoRechazaClienteQueNoEsDueno() {
        when(obtenerUseCase.porEvento(4L)).thenReturn(queryDeCliente(7L));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(99L));

        assertThrows(AccessDeniedException.class, () -> controller.obtenerPorEvento(4L));
    }

    @Test
    void testObtenerPorEventoRechazaClienteSinPerfilAsociado() {
        when(obtenerUseCase.porEvento(4L)).thenReturn(queryDeCliente(7L));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.obtenerPorEvento(4L));
    }

    @Test
    void testObtenerPorEventoPermiteAlStaffSinVerificarPropiedad() {
        when(obtenerUseCase.porEvento(4L)).thenReturn(queryDeCliente(7L));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);

        ResponseEntity<?> respuesta = controller.obtenerPorEvento(4L);

        assertEquals(200, respuesta.getStatusCode().value());
        verify(supabaseAuthFacade, never()).clientePerfilId();
    }

    @Test
    void testDescargarRechazaClienteQueNoEsDueno() {
        when(obtenerUseCase.porEvento(4L)).thenReturn(queryDeCliente(7L));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(99L));

        assertThrows(AccessDeniedException.class, () -> controller.descargar(4L));
        verify(descargarUseCase, never()).ejecutar(anyLong());
    }

    @Test
    void testDescargarPermiteAlClienteDuenoYRetornaElArchivo() {
        when(obtenerUseCase.porEvento(4L)).thenReturn(queryDeCliente(7L));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(7L));
        when(descargarUseCase.ejecutar(4L)).thenReturn(ArchivoContratoQuery.builder()
                .contenido(new byte[]{1, 2, 3})
                .nombreArchivo("contrato-evento-4.pdf")
                .build());

        ResponseEntity<byte[]> respuesta = controller.descargar(4L);

        assertEquals(200, respuesta.getStatusCode().value());
        assertArrayEquals(new byte[]{1, 2, 3}, respuesta.getBody());
    }

    @Test
    void testCargarPropagaUsuarioAutenticadoAlComando() {
        UUID idAdmin = UUID.randomUUID();
        when(supabaseAuthFacade.usuarioActualId()).thenReturn(Optional.of(idAdmin));
        when(cargarUseCase.ejecutar(any())).thenReturn(queryDeCliente(7L));

        org.springframework.mock.web.MockMultipartFile archivo =
                new org.springframework.mock.web.MockMultipartFile(
                        "archivo", "contrato.pdf", "application/pdf",
                        "%PDF-1.4".getBytes());

        ResponseEntity<?> respuesta = controller.cargar(4L, archivo);

        assertEquals(201, respuesta.getStatusCode().value());
        verify(cargarUseCase).ejecutar(argThat(cmd ->
                cmd.getIdEventoPrivado().equals(4L)
                        && cmd.getIdUsuarioCarga().equals(idAdmin)
                        && cmd.getContentType().equals("application/pdf")));
    }

    @Test
    void testCargarLanzaExcepcionSiNoHayUsuarioAutenticado() {
        when(supabaseAuthFacade.usuarioActualId()).thenReturn(Optional.empty());

        org.springframework.mock.web.MockMultipartFile archivo =
                new org.springframework.mock.web.MockMultipartFile(
                        "archivo", "contrato.pdf", "application/pdf",
                        "%PDF-1.4".getBytes());

        assertThrows(ResponseStatusException.class, () -> controller.cargar(4L, archivo));
        verify(cargarUseCase, never()).ejecutar(any());
    }
}
