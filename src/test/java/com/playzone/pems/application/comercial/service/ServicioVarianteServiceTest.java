package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.ActualizarServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioVarianteQuery;
import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.BusinessException;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioVarianteServiceTest {

    @Mock private ServicioVarianteRepository repository;
    @Mock private ServicioCotizacionRepository servicioRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;

    private ServicioVarianteService service;

    private ServicioCotizacion servicioPrueba() {
        return ServicioCotizacion.builder().id(1L).nombre("Torta").precioReferencial(BigDecimal.ZERO)
                .activo(true).orden(0).build();
    }

    @Test
    void testCrearVarianteGuardaYAuditaCorrectamente() {
        service = new ServicioVarianteService(repository, servicioRepository, authFacade, auditoria);
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));
        when(repository.existsByServicioAndNombre(1L, "Pequeña")).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            ServicioVariante arg = inv.getArgument(0);
            return arg.toBuilder().id(10L).build();
        });

        CrearServicioVarianteCommand comando = CrearServicioVarianteCommand.builder()
                .nombre("Pequeña").precio(new BigDecimal("80.00")).activo(true).orden(0).build();

        ServicioVarianteQuery resultado = service.crear(1L, comando);

        assertEquals(10L, resultado.getId());
        assertEquals("Pequeña", resultado.getNombre());
        verify(auditoria).ejecutar(any(RegistrarLogUseCase.Command.class));
    }

    @Test
    void testCrearVarianteConServicioInexistenteLanzaResourceNotFoundException() {
        service = new ServicioVarianteService(repository, servicioRepository, authFacade, auditoria);
        when(servicioRepository.findById(99L)).thenReturn(Optional.empty());

        CrearServicioVarianteCommand comando = CrearServicioVarianteCommand.builder()
                .nombre("Pequeña").precio(BigDecimal.TEN).build();

        assertThrows(ResourceNotFoundException.class, () -> service.crear(99L, comando));
        verify(repository, never()).save(any());
    }

    @Test
    void testCrearVarianteConNombreDuplicadoLanzaBusinessException() {
        service = new ServicioVarianteService(repository, servicioRepository, authFacade, auditoria);
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicioPrueba()));
        when(repository.existsByServicioAndNombre(1L, "Pequeña")).thenReturn(true);

        CrearServicioVarianteCommand comando = CrearServicioVarianteCommand.builder()
                .nombre("Pequeña").precio(BigDecimal.TEN).build();

        assertThrows(BusinessException.class, () -> service.crear(1L, comando));
        verify(repository, never()).save(any());
    }

    @Test
    void testActualizarVarianteDeOtroServicioLanzaResourceNotFoundException() {
        service = new ServicioVarianteService(repository, servicioRepository, authFacade, auditoria);
        ServicioVariante existente = ServicioVariante.builder()
                .id(10L).idServicio(1L).nombre("Pequeña").precio(BigDecimal.TEN).activo(true).orden(0).build();
        when(repository.findById(10L)).thenReturn(Optional.of(existente));

        ActualizarServicioVarianteCommand comando = ActualizarServicioVarianteCommand.builder()
                .id(10L).nombre("Pequeña").precio(BigDecimal.TEN).build();

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(2L, comando));
    }

    @Test
    void testEliminarVarianteDelegaEnRepositorioYAudita() {
        service = new ServicioVarianteService(repository, servicioRepository, authFacade, auditoria);
        ServicioVariante existente = ServicioVariante.builder()
                .id(10L).idServicio(1L).nombre("Pequeña").precio(BigDecimal.TEN).activo(true).orden(0).build();
        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());

        service.eliminar(1L, 10L);

        verify(repository).deleteById(10L);
        verify(auditoria).ejecutar(any(RegistrarLogUseCase.Command.class));
    }

    @Test
    void testReordenarIntercambiaOrdenConLaVarianteExistente() {
        service = new ServicioVarianteService(repository, servicioRepository, authFacade, auditoria);
        ServicioVariante primera = ServicioVariante.builder()
                .id(10L).idServicio(1L).nombre("Pequeña").precio(BigDecimal.TEN).activo(true).orden(0).build();
        ServicioVariante segunda = ServicioVariante.builder()
                .id(11L).idServicio(1L).nombre("Grande").precio(BigDecimal.TEN).activo(true).orden(1).build();
        when(repository.findById(10L)).thenReturn(Optional.of(primera));
        when(repository.findByServicio(1L)).thenReturn(List.of(primera, segunda));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reordenar(1L, 10L, 1);

        ArgumentCaptor<ServicioVariante> captor = ArgumentCaptor.forClass(ServicioVariante.class);
        verify(repository, times(2)).save(captor.capture());
        List<ServicioVariante> guardadas = captor.getAllValues();
        ServicioVariante guardadaSegunda = guardadas.stream().filter(v -> v.getId().equals(11L)).findFirst().orElseThrow();
        ServicioVariante guardadaPrimera = guardadas.stream().filter(v -> v.getId().equals(10L)).findFirst().orElseThrow();
        assertEquals(0, guardadaSegunda.getOrden());
        assertEquals(1, guardadaPrimera.getOrden());
    }
}
