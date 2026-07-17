package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.ActualizarServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioCotizacionQuery;
import com.playzone.pems.domain.comercial.model.CategoriaServicio;
import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.CategoriaServicioRepository;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioImagenRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCotizacionServiceTest {

    @Mock private ServicioCotizacionRepository repository;
    @Mock private ServicioVarianteRepository varianteRepository;
    @Mock private ServicioImagenRepository imagenRepository;
    @Mock private CategoriaServicioRepository categoriaRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;

    private ServicioCotizacionService service;

    private CrearServicioCommand.CrearServicioCommandBuilder comandoCrearBase() {
        return CrearServicioCommand.builder()
                .nombre("Show de Titeres")
                .descripcion("Animacion infantil")
                .precioReferencial(new BigDecimal("150.00"))
                .icono("Package")
                .activo(true)
                .orden(0);
    }

    @Test
    void testCrearServicioGuardaYAuditaCorrectamente() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(repository.existsByNombre("Show de Titeres")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            ServicioCotizacion arg = inv.getArgument(0);
            return arg.toBuilder().id(1L).build();
        });

        ServicioCotizacionQuery resultado = service.crear(comandoCrearBase().build());

        assertEquals(1L, resultado.getId());
        assertEquals("Show de Titeres", resultado.getNombre());
        verify(auditoria).ejecutar(any(RegistrarLogUseCase.Command.class));
    }

    @Test
    void testCrearServicioConNombreDuplicadoLanzaBusinessException() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        when(repository.existsByNombre("Show de Titeres")).thenReturn(true);

        CrearServicioCommand comando = comandoCrearBase().build();

        assertThrows(BusinessException.class, () -> service.crear(comando));
        verify(repository, never()).save(any());
    }

    @Test
    void testCrearServicioConCategoriaInexistenteLanzaBusinessException() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        when(repository.existsByNombre("Show de Titeres")).thenReturn(false);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        CrearServicioCommand comando = comandoCrearBase().categoriaId(99L).build();

        assertThrows(BusinessException.class, () -> service.crear(comando));
        verify(repository, never()).save(any());
    }

    @Test
    void testCrearServicioConCategoriaYDestacadoValidosSeAsignanCorrectamente() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        CategoriaServicio categoria = CategoriaServicio.builder().id(5L).nombre("Animacion").activo(true).orden(0).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(repository.existsByNombre("Show de Titeres")).thenReturn(false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(repository.save(any())).thenAnswer(inv -> {
            ServicioCotizacion arg = inv.getArgument(0);
            return arg.toBuilder().id(1L).build();
        });

        ServicioCotizacionQuery resultado = service.crear(comandoCrearBase().categoriaId(5L).destacado(true).build());

        assertEquals(5L, resultado.getCategoriaId());
        assertEquals("Animacion", resultado.getCategoriaNombre());
        assertTrue(resultado.isDestacado());
    }

    @Test
    void testActualizarServicioInexistenteLanzaResourceNotFoundException() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ActualizarServicioCommand comando = ActualizarServicioCommand.builder()
                .id(99L).nombre("X").precioReferencial(BigDecimal.ZERO).orden(0).build();

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(comando));
    }

    @Test
    void testActualizarServicioConNombreDuplicadoExcluyendoElPropioLanzaBusinessException() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        ServicioCotizacion existente = ServicioCotizacion.builder()
                .id(1L).nombre("Show de Titeres").precioReferencial(BigDecimal.TEN).activo(true).orden(0).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreExcludingId("Decoracion", 1L)).thenReturn(true);

        ActualizarServicioCommand comando = ActualizarServicioCommand.builder()
                .id(1L).nombre("Decoracion").precioReferencial(BigDecimal.TEN).orden(0).build();

        assertThrows(BusinessException.class, () -> service.actualizar(comando));
        verify(repository, never()).save(any());
    }

    @Test
    void testActualizarServicioConMismoNombrePropioNoLanzaError() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        ServicioCotizacion existente = ServicioCotizacion.builder()
                .id(1L).nombre("Show de Titeres").precioReferencial(BigDecimal.TEN).activo(true).orden(0).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreExcludingId("Show de Titeres", 1L)).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActualizarServicioCommand comando = ActualizarServicioCommand.builder()
                .id(1L).nombre("Show de Titeres").precioReferencial(new BigDecimal("200.00")).orden(1).activo(false).build();

        ServicioCotizacionQuery resultado = service.actualizar(comando);

        assertEquals(new BigDecimal("200.00"), resultado.getPrecioReferencial());
        assertEquals(1, resultado.getOrden());
    }

    @Test
    void testEliminarServicioInexistenteLanzaResourceNotFoundException() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(5L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void testEliminarServicioExistenteDelegaEnRepositorioYAudita() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        ServicioCotizacion existente = ServicioCotizacion.builder()
                .id(1L).nombre("Show de Titeres").precioReferencial(BigDecimal.TEN).activo(true).orden(0).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());

        service.eliminar(1L);

        verify(repository).deleteById(1L);
        ArgumentCaptor<RegistrarLogUseCase.Command> captor = ArgumentCaptor.forClass(RegistrarLogUseCase.Command.class);
        verify(auditoria).ejecutar(captor.capture());
        assertEquals("ELIMINAR", captor.getValue().accion());
    }

    @Test
    void testListarActivosDelegaEnRepositorio() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        ServicioCotizacion activo = ServicioCotizacion.builder()
                .id(1L).nombre("Show de Titeres").precioReferencial(BigDecimal.TEN).activo(true).orden(0).build();
        when(repository.findAllActivos()).thenReturn(java.util.List.of(activo));

        assertEquals(1, service.listarActivos().size());
    }

    @Test
    void testListarActivosSinVariantesExponePrecioReferencialComoPrecioDesde() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        ServicioCotizacion servicio = ServicioCotizacion.builder()
                .id(1L).nombre("Torta").precioReferencial(new BigDecimal("50.00")).activo(true).orden(0).build();
        when(repository.findAllActivos()).thenReturn(java.util.List.of(servicio));
        when(varianteRepository.findByServicios(java.util.List.of(1L))).thenReturn(java.util.Map.of());

        ServicioCotizacionQuery resultado = service.listarActivos().get(0);

        assertFalse(resultado.isTieneVariantes());
        assertEquals(new BigDecimal("50.00"), resultado.getPrecioDesde());
    }

    @Test
    void testListarActivosConVariantesExponePrecioMinimoComoPrecioDesde() {
        service = new ServicioCotizacionService(repository, varianteRepository, imagenRepository, categoriaRepository, authFacade, auditoria);
        ServicioCotizacion servicio = ServicioCotizacion.builder()
                .id(1L).nombre("Torta").precioReferencial(new BigDecimal("50.00")).activo(true).orden(0).build();
        ServicioVariante pequena = ServicioVariante.builder()
                .id(10L).idServicio(1L).nombre("Pequeña").precio(new BigDecimal("80.00")).activo(true).orden(0).build();
        ServicioVariante grande = ServicioVariante.builder()
                .id(11L).idServicio(1L).nombre("Grande").precio(new BigDecimal("120.00")).activo(true).orden(1).build();
        when(repository.findAllActivos()).thenReturn(java.util.List.of(servicio));
        when(varianteRepository.findByServicios(java.util.List.of(1L)))
                .thenReturn(java.util.Map.of(1L, java.util.List.of(pequena, grande)));

        ServicioCotizacionQuery resultado = service.listarActivos().get(0);

        assertTrue(resultado.isTieneVariantes());
        assertEquals(new BigDecimal("80.00"), resultado.getPrecioDesde());
        assertEquals(2, resultado.getVariantes().size());
    }
}
