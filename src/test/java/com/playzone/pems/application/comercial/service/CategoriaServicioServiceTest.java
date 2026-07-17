package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.ActualizarCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.query.CategoriaServicioQuery;
import com.playzone.pems.domain.comercial.model.CategoriaServicio;
import com.playzone.pems.domain.comercial.repository.CategoriaServicioRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.BusinessException;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServicioServiceTest {

    @Mock private CategoriaServicioRepository repository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;

    private CategoriaServicioService service;

    @Test
    void testCrearCategoriaGuardaYAuditaCorrectamente() {
        service = new CategoriaServicioService(repository, authFacade, auditoria);
        when(repository.existsByNombre("Animacion")).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            CategoriaServicio arg = inv.getArgument(0);
            return arg.toBuilder().id(1L).build();
        });

        CategoriaServicioQuery resultado = service.crear(
                CrearCategoriaServicioCommand.builder().nombre("Animacion").activo(true).orden(0).build());

        assertEquals(1L, resultado.getId());
        assertEquals("Animacion", resultado.getNombre());
        verify(auditoria).ejecutar(any(RegistrarLogUseCase.Command.class));
    }

    @Test
    void testCrearCategoriaConNombreDuplicadoLanzaBusinessException() {
        service = new CategoriaServicioService(repository, authFacade, auditoria);
        when(repository.existsByNombre("Animacion")).thenReturn(true);

        CrearCategoriaServicioCommand comando =
                CrearCategoriaServicioCommand.builder().nombre("Animacion").activo(true).orden(0).build();

        assertThrows(BusinessException.class, () -> service.crear(comando));
        verify(repository, never()).save(any());
    }

    @Test
    void testActualizarCategoriaInexistenteLanzaResourceNotFoundException() {
        service = new CategoriaServicioService(repository, authFacade, auditoria);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ActualizarCategoriaServicioCommand comando =
                ActualizarCategoriaServicioCommand.builder().id(99L).nombre("X").orden(0).build();

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(comando));
    }

    @Test
    void testEliminarCategoriaConServiciosAsociadosLanzaBusinessException() {
        service = new CategoriaServicioService(repository, authFacade, auditoria);
        CategoriaServicio existente = CategoriaServicio.builder().id(1L).nombre("Animacion").activo(true).orden(0).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.tieneServiciosAsociados(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.eliminar(1L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void testEliminarCategoriaSinServiciosAsociadosDelegaEnRepositorio() {
        service = new CategoriaServicioService(repository, authFacade, auditoria);
        CategoriaServicio existente = CategoriaServicio.builder().id(1L).nombre("Animacion").activo(true).orden(0).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.tieneServiciosAsociados(1L)).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());

        service.eliminar(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void testListarActivasDelegaEnRepositorio() {
        service = new CategoriaServicioService(repository, authFacade, auditoria);
        CategoriaServicio activa = CategoriaServicio.builder().id(1L).nombre("Animacion").activo(true).orden(0).build();
        when(repository.findAllActivas()).thenReturn(List.of(activa));

        assertEquals(1, service.listarActivas().size());
    }
}
