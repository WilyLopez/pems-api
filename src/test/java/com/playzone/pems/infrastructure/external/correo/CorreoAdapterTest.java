package com.playzone.pems.infrastructure.external.correo;

import com.playzone.pems.application.cms.port.in.GestionarConfiguracionPublicaUseCase;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.NotaVentaPdfService;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoAdapterTest {

    @Mock private JavaMailCorreoClient correoClient;
    @Mock private JavaMailSender mailSender;
    @Mock private TicketIngresoPdfService ticketIngresoPdfService;
    @Mock private NotaVentaPdfService notaVentaPdfService;
    @Mock private SedeRepository sedeRepository;
    @Mock private TemplateService templateService;
    @Mock private GestionarConfiguracionPublicaUseCase configuracionPublica;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;

    private CorreoAdapter correoAdapter;

    @BeforeEach
    void setUp() {
        correoAdapter = new CorreoAdapter(
                correoClient, mailSender, ticketIngresoPdfService, notaVentaPdfService,
                sedeRepository, templateService, configuracionPublica,
                resolverAdministradoresPort, perfilUsuarioRepository);
    }

    private void configurarUnAdministrador(String correo) {
        UUID idAdmin = UUID.randomUUID();
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(idAdmin));
        when(perfilUsuarioRepository.buscarPorId(idAdmin)).thenReturn(
                Optional.of(PerfilUsuario.builder().id(idAdmin).correo(correo).build()));
    }

    @Test
    void testNotificarAdminNuevaSolicitudEscapaNombreClienteMalicioso() {
        configurarUnAdministrador("admin@kikiylala.lat");

        EventoPrivadoQuery evento = EventoPrivadoQuery.builder()
                .nombreCliente("<img src=x onerror=alert(1)>")
                .correoCliente("atacante@correo.com")
                .telefonoCliente("999999999")
                .tipoEvento("CUMPLEANOS")
                .fechaEvento(null)
                .turno("MANANA")
                .horaInicio("09:00")
                .horaFin("12:00")
                .build();

        correoAdapter.notificarAdminNuevaSolicitud(evento);

        ArgumentCaptor<String> cuerpoCaptor = ArgumentCaptor.forClass(String.class);
        verify(correoClient).enviar(eq("admin@kikiylala.lat"), any(String.class), cuerpoCaptor.capture());

        String cuerpoEnviado = cuerpoCaptor.getValue();
        assertFalse(cuerpoEnviado.contains("<img src=x onerror=alert(1)>"));
        assertTrue(cuerpoEnviado.contains("&lt;img src=x onerror=alert(1)&gt;"));
    }

    @Test
    void testNotificarAdminNuevaSolicitudEnviaAUnCorreoPorCadaAdministrador() {
        UUID idAdmin1 = UUID.randomUUID();
        UUID idAdmin2 = UUID.randomUUID();
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos())
                .thenReturn(List.of(idAdmin1, idAdmin2));
        when(perfilUsuarioRepository.buscarPorId(idAdmin1)).thenReturn(
                Optional.of(PerfilUsuario.builder().id(idAdmin1).correo("admin1@kikiylala.lat").build()));
        when(perfilUsuarioRepository.buscarPorId(idAdmin2)).thenReturn(
                Optional.of(PerfilUsuario.builder().id(idAdmin2).correo("admin2@kikiylala.lat").build()));

        EventoPrivadoQuery evento = EventoPrivadoQuery.builder()
                .nombreCliente("Juan Perez").fechaEvento(null).build();

        correoAdapter.notificarAdminNuevaSolicitud(evento);

        verify(correoClient).enviar(eq("admin1@kikiylala.lat"), any(String.class), any(String.class));
        verify(correoClient).enviar(eq("admin2@kikiylala.lat"), any(String.class), any(String.class));
    }

    @Test
    void testNotificarAdminNuevaSolicitudSinAdministradoresNoEnviaNada() {
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        EventoPrivadoQuery evento = EventoPrivadoQuery.builder()
                .nombreCliente("Juan Perez").fechaEvento(null).build();

        correoAdapter.notificarAdminNuevaSolicitud(evento);

        verify(correoClient, org.mockito.Mockito.never())
                .enviar(any(String.class), any(String.class), any(String.class));
    }

}
