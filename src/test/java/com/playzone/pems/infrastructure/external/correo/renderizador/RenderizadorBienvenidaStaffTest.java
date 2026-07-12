package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.model.StaffPerfil;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.domain.usuario.repository.UsuarioRolRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorBienvenidaStaffTest {

    @Mock private StaffPerfilRepository staffPerfilRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;

    private RenderizadorBienvenidaStaff renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorBienvenidaStaff(
                staffPerfilRepository, perfilUsuarioRepository, sedeRepository,
                usuarioRolRepository, new TemplateService(), new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void testTipoCodigoEsUsuarioActivacion() {
        assertEquals("USUARIO_ACTIVACION", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConEnlaceDeActivacionYRolAdministrador() {
        UUID usuarioId = UUID.randomUUID();
        when(staffPerfilRepository.buscarPorId(20L)).thenReturn(Optional.of(
                StaffPerfil.builder().id(20L).usuarioId(usuarioId).sedeId(2L).build()));
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                PerfilUsuario.builder().id(usuarioId).nombreCompleto("Jose Rios").correo("jose@correo.com").build()));
        when(sedeRepository.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).nombre("Sede Sur").build()));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(usuarioId)).thenReturn(List.of("ADMIN"));

        Notificacion notificacion = Notificacion.builder()
                .entidadId(20L)
                .metadata("{\"tokenActivacion\":\"raw-token-123\"}")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("jose@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Jose Rios"));
        assertTrue(resultado.getCuerpoHtml().contains("Administrador"));
        assertTrue(resultado.getCuerpoHtml().contains("token=raw-token-123"));
    }

    @Test
    void testRenderizarSinTokenEnMetadataLanzaExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(staffPerfilRepository.buscarPorId(21L)).thenReturn(Optional.of(
                StaffPerfil.builder().id(21L).usuarioId(usuarioId).sedeId(2L).build()));
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                PerfilUsuario.builder().id(usuarioId).nombreCompleto("Jose Rios").correo("jose@correo.com").build()));
        when(sedeRepository.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).nombre("Sede Sur").build()));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(usuarioId)).thenReturn(List.of("CAJERO"));

        Notificacion notificacion = Notificacion.builder().entidadId(21L).build();

        assertThrows(IllegalStateException.class, () -> renderizador.renderizar(notificacion));
    }
}
