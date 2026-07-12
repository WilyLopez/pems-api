package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorCajaCierreDiscrepanciaTest {

    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;

    private RenderizadorCajaCierreDiscrepancia renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorCajaCierreDiscrepancia(perfilUsuarioRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsCajaCierreDiscrepancia() {
        assertEquals("CAJA_CIERRE_DISCREPANCIA", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaUsandoCorreoDelAdministrador() {
        UUID adminId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(adminId)).thenReturn(Optional.of(
                PerfilUsuario.builder().id(adminId).nombreCompleto("Admin").correo("admin@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioUsuarioId(adminId)
                .titulo("Discrepancia en caja — Sede Norte")
                .mensaje("El cierre de caja en Sede Norte tiene una diferencia de S/ 50.00.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("admin@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("50.00"));
    }
}
