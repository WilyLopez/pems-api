package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.SubirServicioImagenCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioImagenQuery;
import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioImagen;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioImagenRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.BusinessException;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioImagenServiceTest {

    @Mock private ServicioImagenRepository     repository;
    @Mock private ServicioCotizacionRepository servicioRepository;
    @Mock private ServicioVarianteRepository   varianteRepository;
    @Mock private StoragePort                  storagePort;
    @Mock private SupabaseAuthFacade           authFacade;
    @Mock private RegistrarLogUseCase          auditoria;

    private ServicioImagenService service;

    private ServicioCotizacion servicioPrueba() {
        return ServicioCotizacion.builder().id(1L).nombre("Torta").precioReferencial(BigDecimal.ZERO)
                .activo(true).orden(0).build();
    }

    private SubirServicioImagenCommand.SubirServicioImagenCommandBuilder comandoBase() {
        return SubirServicioImagenCommand.builder()
                .contenido(new byte[]{1, 2, 3})
                .nombreArchivo("torta.png")
                .contentType("image/png")
                .orden(0);
    }

    @Test
    void testSubirImagenValidaGuardaYAuditaCorrectamente() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));
        when(repository.countByServicioSinVariante(1L)).thenReturn(0L);
        when(storagePort.upload(anyString(), anyString(), any(), anyString())).thenReturn("https://storage/servicios/torta.png");
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            ServicioImagen arg = inv.getArgument(0);
            return arg.toBuilder().id(20L).build();
        });

        ServicioImagenQuery resultado = service.subir(1L, comandoBase().build());

        assertEquals(20L, resultado.getId());
        assertEquals("https://storage/servicios/torta.png", resultado.getUrl());
        verify(auditoria).ejecutar(any(RegistrarLogUseCase.Command.class));
    }

    @Test
    void testSubirImagenConServicioInexistenteLanzaResourceNotFoundException() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        when(servicioRepository.findById(99L)).thenReturn(Optional.empty());

        SubirServicioImagenCommand comando = comandoBase().build();

        assertThrows(ResourceNotFoundException.class, () -> service.subir(99L, comando));
        verify(storagePort, never()).upload(anyString(), anyString(), any(), anyString());
    }

    @Test
    void testSubirImagenConTipoNoPermitidoLanzaBusinessException() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));

        SubirServicioImagenCommand comando = comandoBase().contentType("image/svg+xml").build();

        assertThrows(BusinessException.class, () -> service.subir(1L, comando));
        verify(storagePort, never()).upload(anyString(), anyString(), any(), anyString());
    }

    @Test
    void testSubirImagenQueSuperaTamanoMaximoLanzaBusinessException() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));

        byte[] archivoGrande = new byte[9 * 1024 * 1024];
        SubirServicioImagenCommand comando = comandoBase().contenido(archivoGrande).build();

        assertThrows(BusinessException.class, () -> service.subir(1L, comando));
        verify(storagePort, never()).upload(anyString(), anyString(), any(), anyString());
    }

    @Test
    void testSubirImagenAlcanzarLimiteDeCantidadLanzaBusinessException() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));
        when(repository.countByServicioSinVariante(1L)).thenReturn(8L);

        SubirServicioImagenCommand comando = comandoBase().build();

        assertThrows(BusinessException.class, () -> service.subir(1L, comando));
        verify(storagePort, never()).upload(anyString(), anyString(), any(), anyString());
    }

    @Test
    void testSubirImagenConVarianteDeOtroServicioLanzaResourceNotFoundException() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));
        ServicioVariante variante = ServicioVariante.builder()
                .id(100L).idServicio(2L).nombre("Grande").precio(BigDecimal.TEN).activo(true).orden(0).build();
        when(varianteRepository.findById(100L)).thenReturn(Optional.of(variante));

        SubirServicioImagenCommand comando = comandoBase().idVariante(100L).build();

        assertThrows(ResourceNotFoundException.class, () -> service.subir(1L, comando));
        verify(storagePort, never()).upload(anyString(), anyString(), any(), anyString());
    }

    @Test
    void testEliminarImagenBorraDeStorageYRepositorio() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        ServicioImagen imagen = ServicioImagen.builder()
                .id(20L).idServicio(1L).archivoPath("https://storage/servicios/torta.png").orden(0).build();
        when(repository.findById(20L)).thenReturn(Optional.of(imagen));

        service.eliminar(1L, 20L);

        verify(storagePort).deleteByUrl("https://storage/servicios/torta.png");
        verify(repository).deleteById(20L);
    }

    @Test
    void testEliminarImagenDeOtroServicioLanzaResourceNotFoundException() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        ServicioImagen imagen = ServicioImagen.builder()
                .id(20L).idServicio(2L).archivoPath("https://storage/servicios/torta.png").orden(0).build();
        when(repository.findById(20L)).thenReturn(Optional.of(imagen));

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(1L, 20L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void testMarcarPrincipalLimpiaAnteriorYMarcaNueva() {
        service = new ServicioImagenService(repository, servicioRepository, varianteRepository,
                storagePort, authFacade, auditoria, "kiki-publico");
        ServicioImagen imagen = ServicioImagen.builder()
                .id(20L).idServicio(1L).archivoPath("https://storage/servicios/torta.png")
                .orden(0).esPrincipal(false).build();
        when(repository.findById(20L)).thenReturn(Optional.of(imagen));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServicioImagenQuery resultado = service.marcarPrincipal(1L, 20L);

        verify(repository).limpiarPrincipal(1L, null);
        assertTrue(resultado.isEsPrincipal());
    }
}
