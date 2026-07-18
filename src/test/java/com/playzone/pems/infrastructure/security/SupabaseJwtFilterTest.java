package com.playzone.pems.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.domain.usuario.repository.UsuarioRolRepository;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.security.KeyPair;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseJwtFilterTest {

    private static final String KID = "test-kid";

    @Mock private SupabaseJwksProvider jwksProvider;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private StaffPerfilRepository staffPerfilRepository;
    @Mock private FilterChain filterChain;

    private SupabaseJwtFilter filter;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() {
        filter = new SupabaseJwtFilter(jwksProvider, usuarioRolRepository, perfilUsuarioRepository,
                clientePerfilRepository, staffPerfilRepository, new ObjectMapper());
        keyPair = Jwts.SIG.ES256.keyPair().build();
    }

    private String tokenPara(UUID userId) {
        when(jwksProvider.resolverClavePorKid(KID)).thenReturn(keyPair.getPublic());
        return Jwts.builder()
                .header().add("kid", KID).and()
                .subject(userId.toString())
                .audience().add("authenticated").and()
                .claim("email", "cliente@correo.com")
                .claim("role", "authenticated")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
                .compact();
    }

    @Test
    void clienteDesactivadoRecibe403ConAccountInactive() throws Exception {
        UUID userId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(userId))
                .thenReturn(Optional.of(PerfilUsuario.builder().id(userId).build()));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(userId)).thenReturn(List.of("CLIENTE"));
        when(usuarioRolRepository.listarCodigosPermisoPorUsuario(userId)).thenReturn(List.of());
        when(clientePerfilRepository.buscarPorUsuarioIdIncluyendoInactivos(userId)).thenReturn(
                Optional.of(ClientePerfil.builder().id(5L).usuarioId(userId)
                        .deletedAt(OffsetDateTime.now()).build()));
        when(staffPerfilRepository.buscarPorUsuarioId(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenPara(userId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("account_inactive"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void clienteActivoContinuaLaCadenaDeFiltros() throws Exception {
        UUID userId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(userId))
                .thenReturn(Optional.of(PerfilUsuario.builder().id(userId).build()));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(userId)).thenReturn(List.of("CLIENTE"));
        when(usuarioRolRepository.listarCodigosPermisoPorUsuario(userId)).thenReturn(List.of());
        when(clientePerfilRepository.buscarPorUsuarioIdIncluyendoInactivos(userId)).thenReturn(
                Optional.of(ClientePerfil.builder().id(5L).usuarioId(userId).deletedAt(null).build()));
        when(staffPerfilRepository.buscarPorUsuarioId(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenPara(userId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(eq(request), eq(response));
    }

    @Test
    void clienteSinPerfilCompletadoAunPuedeAutenticarse() throws Exception {
        UUID userId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(userId))
                .thenReturn(Optional.of(PerfilUsuario.builder().id(userId).build()));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(userId)).thenReturn(List.of("CLIENTE"));
        when(usuarioRolRepository.listarCodigosPermisoPorUsuario(userId)).thenReturn(List.of());
        when(clientePerfilRepository.buscarPorUsuarioIdIncluyendoInactivos(userId)).thenReturn(Optional.empty());
        when(staffPerfilRepository.buscarPorUsuarioId(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenPara(userId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(eq(request), eq(response));
    }
}
