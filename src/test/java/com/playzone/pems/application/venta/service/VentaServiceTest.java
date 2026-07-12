package com.playzone.pems.application.venta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.venta.port.out.EnviarDocumentosVentaPort;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private ReservaPublicaRepository reservaPublicaRepository;
    @Mock private EnviarDocumentosVentaPort enviarDocumentosVentaPort;
    @Mock private ConfiguracionCalendarioRepository configRepository;
    @Mock private EnrutadorCajaService enrutadorCajaService;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private CrearNotificacionPort crearNotificacionPort;

    private VentaService service;

    @BeforeEach
    void setUp() {
        service = new VentaService(
                ventaRepository, ventaPagoRepository, clientePerfilRepository, reservaPublicaRepository,
                enviarDocumentosVentaPort, configRepository, enrutadorCajaService, auditoria,
                crearNotificacionPort, new ObjectMapper());
    }

    private Venta ventaConCliente() {
        return Venta.builder().id(900L).idSede(1L).clienteId(12L)
                .total(new BigDecimal("80.00")).build();
    }

    @Test
    void testEnviarCorreoVentaConClienteUsaNotificarTransaccionalYPreservaCorreoManual() {
        when(ventaRepository.findById(900L)).thenReturn(Optional.of(ventaConCliente()));
        when(clientePerfilRepository.buscarPorId(12L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(12L).nombres("Pedro").correo("registrado@correo.com").build()));
        when(reservaPublicaRepository.findByVentaId(900L)).thenReturn(List.of());
        when(ventaPagoRepository.findByVentaId(900L)).thenReturn(List.of());
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.enviarCorreoVenta(900L, "otro-correo@manual.com");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("DOCUMENTO_LISTO", cmd.getTipoCodigo());
        assertEquals(12L, cmd.getDestinatarioClienteId());
        assertEquals(900L, cmd.getEntidadId());
        assertTrue(cmd.getMetadata().contains("otro-correo@manual.com"));
        verify(enviarDocumentosVentaPort, never()).enviarDocumentos(anyString(), any());

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertTrue(ventaCaptor.getValue().isEnviadoCorreo());
    }

    @Test
    void testEnviarCorreoVentaSinClienteUsaEnvioDirectoSincronico() {
        Venta ventaSinCliente = Venta.builder().id(901L).idSede(1L).clienteId(null)
                .total(new BigDecimal("50.00")).build();
        when(ventaRepository.findById(901L)).thenReturn(Optional.of(ventaSinCliente));
        when(reservaPublicaRepository.findByVentaId(901L)).thenReturn(List.of());
        when(ventaPagoRepository.findByVentaId(901L)).thenReturn(List.of());
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.enviarCorreoVenta(901L, "walkin@correo.com");

        verify(enviarDocumentosVentaPort).enviarDocumentos(eq("walkin@correo.com"), any());
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    @Test
    void testEnviarCorreoVentaSinDestinatarioLanzaValidationException() {
        Venta ventaSinCliente = Venta.builder().id(902L).idSede(1L).clienteId(null)
                .total(new BigDecimal("50.00")).build();
        when(ventaRepository.findById(902L)).thenReturn(Optional.of(ventaSinCliente));
        when(reservaPublicaRepository.findByVentaId(902L)).thenReturn(List.of());
        when(ventaPagoRepository.findByVentaId(902L)).thenReturn(List.of());

        assertThrows(com.playzone.pems.shared.exception.ValidationException.class,
                () -> service.enviarCorreoVenta(902L, null));
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
        verify(enviarDocumentosVentaPort, never()).enviarDocumentos(anyString(), any());
    }
}
