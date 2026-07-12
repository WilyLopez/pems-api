package com.playzone.pems.application.contrato.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.contrato.dto.command.CargarContratoCommand;
import com.playzone.pems.application.contrato.dto.query.ArchivoContratoQuery;
import com.playzone.pems.application.contrato.dto.query.ContratoQuery;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.contrato.exception.ContratoNotFoundException;
import com.playzone.pems.domain.contrato.model.ActividadContrato;
import com.playzone.pems.domain.contrato.model.Contrato;
import com.playzone.pems.domain.contrato.repository.ActividadContratoRepository;
import com.playzone.pems.domain.contrato.repository.ContratoRepository;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock private ContratoRepository          contratoRepository;
    @Mock private ActividadContratoRepository actividadRepository;
    @Mock private StoragePort                 storagePort;
    @Mock private RegistrarLogUseCase         auditoria;
    @Mock private CrearNotificacionPort       crearNotificacionPort;

    private ContratoService service;

    @BeforeEach
    void setUp() {
        service = new ContratoService(
                contratoRepository, actividadRepository, storagePort,
                "kiki-privado", auditoria, crearNotificacionPort);
    }

    private byte[] pdfValido() {
        return "%PDF-1.4 contenido de prueba".getBytes(StandardCharsets.US_ASCII);
    }

    private Contrato contratoExistente() {
        return Contrato.builder()
                .id(10L)
                .idEventoPrivado(4L)
                .idCliente(7L)
                .archivoPdfUrl("https://storage/kiki-privado/contratos/contrato_4_viejo.pdf")
                .tipoEvento("CUMPLEANOS")
                .fechaEvento(LocalDate.of(2026, 8, 1))
                .build();
    }

    @Test
    void testCargarContratoCreaNuevoCuandoEventoNoTieneContrato() {
        when(contratoRepository.findByEventoPrivado(4L)).thenReturn(Optional.empty());
        when(storagePort.upload(eq("kiki-privado"), anyString(), any(), eq("application/pdf")))
                .thenReturn("https://storage/kiki-privado/contratos/contrato_4_nuevo.pdf");
        when(contratoRepository.save(any())).thenAnswer(inv -> {
            Contrato c = inv.getArgument(0);
            return c.toBuilder().id(20L).idCliente(7L).build();
        });
        when(actividadRepository.findByContrato(20L)).thenReturn(List.of());

        UUID idAdmin = UUID.randomUUID();
        ContratoQuery resultado = service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(pdfValido())
                .contentType("application/pdf")
                .idUsuarioCarga(idAdmin)
                .build());

        assertEquals(20L, resultado.getId());
        verify(storagePort, never()).deleteByUrl(anyString());

        ArgumentCaptor<ActividadContrato> actividadCaptor = ArgumentCaptor.forClass(ActividadContrato.class);
        verify(actividadRepository).save(actividadCaptor.capture());
        assertEquals("CARGADO", actividadCaptor.getValue().getAccion());

        ArgumentCaptor<CrearNotificacionCommand> notifCaptor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(notifCaptor.capture());
        assertEquals("EVENTO_CONTRATO_LISTO", notifCaptor.getValue().getTipoCodigo());
        assertEquals(7L, notifCaptor.getValue().getDestinatarioClienteId());
    }

    @Test
    void testCargarContratoReemplazaYEliminaArchivoAnteriorCuandoYaExiste() {
        Contrato actual = contratoExistente();
        when(contratoRepository.findByEventoPrivado(4L)).thenReturn(Optional.of(actual));
        when(storagePort.upload(eq("kiki-privado"), anyString(), any(), eq("application/pdf")))
                .thenReturn("https://storage/kiki-privado/contratos/contrato_4_nuevo.pdf");
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(actividadRepository.findByContrato(10L)).thenReturn(List.of());

        UUID idAdmin = UUID.randomUUID();
        ContratoQuery resultado = service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(pdfValido())
                .contentType("application/pdf")
                .idUsuarioCarga(idAdmin)
                .build());

        assertEquals(10L, resultado.getId());
        verify(storagePort).deleteByUrl(actual.getArchivoPdfUrl());

        ArgumentCaptor<Contrato> guardadoCaptor = ArgumentCaptor.forClass(Contrato.class);
        verify(contratoRepository).save(guardadoCaptor.capture());
        assertEquals("https://storage/kiki-privado/contratos/contrato_4_nuevo.pdf",
                guardadoCaptor.getValue().getArchivoPdfUrl());

        ArgumentCaptor<ActividadContrato> actividadCaptor = ArgumentCaptor.forClass(ActividadContrato.class);
        verify(actividadRepository).save(actividadCaptor.capture());
        assertEquals("REEMPLAZADO", actividadCaptor.getValue().getAccion());
    }

    @Test
    void testCargarContratoRechazaArchivoVacio() {
        assertThrows(ValidationException.class, () -> service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(new byte[0])
                .contentType("application/pdf")
                .idUsuarioCarga(UUID.randomUUID())
                .build()));
        verify(contratoRepository, never()).save(any());
    }

    @Test
    void testCargarContratoRechazaContentTypeDistintoDePdf() {
        assertThrows(ValidationException.class, () -> service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(pdfValido())
                .contentType("image/png")
                .idUsuarioCarga(UUID.randomUUID())
                .build()));
        verify(contratoRepository, never()).save(any());
    }

    @Test
    void testCargarContratoRechazaArchivoSinFirmaPdfValida() {
        byte[] archivoFalso = "esto no es un pdf real".getBytes(StandardCharsets.UTF_8);

        assertThrows(ValidationException.class, () -> service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(archivoFalso)
                .contentType("application/pdf")
                .idUsuarioCarga(UUID.randomUUID())
                .build()));
        verify(contratoRepository, never()).save(any());
    }

    @Test
    void testCargarContratoRechazaArchivoQueSuperaElTamanioMaximo() {
        byte[] archivoGrande = new byte[16 * 1024 * 1024];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, archivoGrande, 0, 5);

        assertThrows(ValidationException.class, () -> service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(archivoGrande)
                .contentType("application/pdf")
                .idUsuarioCarga(UUID.randomUUID())
                .build()));
        verify(contratoRepository, never()).save(any());
    }

    @Test
    void testCargarContratoNoNotificaSiElEventoNoTieneClienteAsociado() {
        when(contratoRepository.findByEventoPrivado(4L)).thenReturn(Optional.empty());
        when(storagePort.upload(anyString(), anyString(), any(), anyString()))
                .thenReturn("https://storage/kiki-privado/contratos/contrato_4_nuevo.pdf");
        when(contratoRepository.save(any())).thenAnswer(inv -> {
            Contrato c = inv.getArgument(0);
            return c.toBuilder().id(21L).idCliente(null).build();
        });
        when(actividadRepository.findByContrato(21L)).thenReturn(List.of());

        service.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(4L)
                .archivo(pdfValido())
                .contentType("application/pdf")
                .idUsuarioCarga(UUID.randomUUID())
                .build());

        verify(crearNotificacionPort, never()).notificar(any());
    }

    @Test
    void testDescargarRetornaBytesDesdeElStorage() {
        Contrato actual = contratoExistente();
        when(contratoRepository.findByEventoPrivado(4L)).thenReturn(Optional.of(actual));
        when(storagePort.downloadByUrl(actual.getArchivoPdfUrl())).thenReturn(pdfValido());

        ArchivoContratoQuery archivo = service.ejecutar(4L);

        assertArrayEquals(pdfValido(), archivo.getContenido());
        assertEquals("contrato-evento-4.pdf", archivo.getNombreArchivo());
    }

    @Test
    void testDescargarLanzaExcepcionSiNoExisteContratoParaElEvento() {
        when(contratoRepository.findByEventoPrivado(99L)).thenReturn(Optional.empty());

        assertThrows(ContratoNotFoundException.class, () -> service.ejecutar(99L));
    }

    @Test
    void testPorEventoLanzaExcepcionSiNoExiste() {
        when(contratoRepository.findByEventoPrivado(99L)).thenReturn(Optional.empty());

        assertThrows(ContratoNotFoundException.class, () -> service.porEvento(99L));
    }

    @Test
    void testPorIdLanzaExcepcionSiNoExiste() {
        when(contratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ContratoNotFoundException.class, () -> service.porId(99L));
    }
}
