package com.playzone.pems.application.marketing.service;

import com.playzone.pems.application.cms.dto.query.ConfiguracionPublicaQuery;
import com.playzone.pems.application.cms.port.in.GestionarConfiguracionPublicaUseCase;
import com.playzone.pems.application.marketing.dto.command.FiltroDestinatariosCommand;
import com.playzone.pems.application.marketing.util.EmailBlockRenderer;
import com.playzone.pems.domain.marketing.model.CampanaEmail;
import com.playzone.pems.domain.marketing.model.EnvioEmail;
import com.playzone.pems.domain.marketing.model.PlantillaEmail;
import com.playzone.pems.domain.marketing.repository.CampanaEmailRepository;
import com.playzone.pems.domain.marketing.repository.EnvioEmailRepository;
import com.playzone.pems.domain.marketing.repository.PlantillaEmailRepository;
import com.playzone.pems.domain.marketing.repository.TipoEmailRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.TokenEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingServiceTest {

    @Mock private PlantillaEmailRepository plantillaRepo;
    @Mock private CampanaEmailRepository campanaRepo;
    @Mock private EnvioEmailRepository envioRepo;
    @Mock private TipoEmailRepository tipoEmailRepo;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private EmailBlockRenderer blockRenderer;
    @Mock private TokenEncryptor tokenEncryptor;
    @Mock private GestionarConfiguracionPublicaUseCase configuracionPublica;

    private MarketingService service;

    @BeforeEach
    void setUp() {
        service = new MarketingService(
                plantillaRepo, campanaRepo, envioRepo, tipoEmailRepo,
                clientePerfilRepository, blockRenderer, tokenEncryptor, configuracionPublica);
        ReflectionTestUtils.setField(service, "urlBase", "https://api.kikiylala.lat");
    }

    private CampanaEmail campanaPendiente() {
        return CampanaEmail.builder().id(1L).idPlantillaEmail(10L).estado("BORRADOR").build();
    }

    private PlantillaEmail plantillaConBloques(String contenidoBloques) {
        return PlantillaEmail.builder().id(10L).asunto("Asunto de prueba").contenidoBloques(contenidoBloques).build();
    }

    private ClientePerfil cliente(long id, String correo) {
        return ClientePerfil.builder().id(id).correo(correo).nombres("Cliente").build();
    }

    @Test
    void testEjecutarLanzaExcepcionSiFaltanVariablesRequeridas() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(campanaPendiente()));
        when(plantillaRepo.findById(10L)).thenReturn(Optional.of(
                plantillaConBloques("[{\"tipo\":\"paragraph\",\"texto\":\"{{promocion}}\"}]")));

        FiltroDestinatariosCommand filtro = FiltroDestinatariosCommand.builder().build();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.ejecutar(1L, filtro));
        assertTrue(ex.getMessage().contains("promocion"));
        verify(envioRepo, never()).guardarTodos(anyList());
    }

    @Test
    void testEjecutarLanzaExcepcionSiPlantillaSinBloques() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(campanaPendiente()));
        when(plantillaRepo.findById(10L)).thenReturn(Optional.of(plantillaConBloques(null)));

        assertThrows(ValidationException.class,
                () -> service.ejecutar(1L, FiltroDestinatariosCommand.builder().build()));
    }

    @Test
    void testEjecutarGeneraEnviosConCuerpoHtmlRenderizado() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(campanaPendiente()));
        when(plantillaRepo.findById(10L)).thenReturn(Optional.of(
                plantillaConBloques("[{\"tipo\":\"paragraph\",\"texto\":\"{{promocion}}\"}]")));
        when(clientePerfilRepository.buscarDestinatariosCampana(any()))
                .thenReturn(List.of(cliente(1L, "cliente1@correo.com"), cliente(2L, "cliente2@correo.com")));
        when(configuracionPublica.obtener()).thenReturn(
                ConfiguracionPublicaQuery.builder().nombreNegocio("Kiki y Lala").build());
        when(tokenEncryptor.encrypt(anyString())).thenReturn("token-cifrado");
        when(blockRenderer.renderizar(anyString(), anyMap(), anyString())).thenReturn("<p>contenido final</p>");

        FiltroDestinatariosCommand filtro = FiltroDestinatariosCommand.builder()
                .valoresVariables(Map.of("promocion", "Pack Verano"))
                .build();

        service.ejecutar(1L, filtro);

        ArgumentCaptor<List<EnvioEmail>> captor = ArgumentCaptor.forClass(List.class);
        verify(envioRepo).guardarTodos(captor.capture());
        List<EnvioEmail> guardados = captor.getValue();

        assertEquals(2, guardados.size());
        assertEquals("<p>contenido final</p>", guardados.get(0).getCuerpoHtml());
        verify(campanaRepo).actualizarEstado(1L, "ENVIANDO");
    }

    @Test
    void testEjecutarLanzaExcepcionSiSuperaLimiteDeDestinatarios() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(campanaPendiente()));
        when(plantillaRepo.findById(10L)).thenReturn(Optional.of(plantillaConBloques("[]")));

        List<ClientePerfil> muchos = new ArrayList<>();
        for (long i = 0; i < 5001; i++) {
            muchos.add(cliente(i, "cliente" + i + "@correo.com"));
        }
        when(clientePerfilRepository.buscarDestinatariosCampana(any())).thenReturn(muchos);

        assertThrows(ValidationException.class,
                () -> service.ejecutar(1L, FiltroDestinatariosCommand.builder().build()));
        verify(envioRepo, never()).guardarTodos(anyList());
    }

    @Test
    void testEjecutarNoPermiteEnviarCampanaYaEnviada() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(
                CampanaEmail.builder().id(1L).idPlantillaEmail(10L).estado("FINALIZADA").build()));

        assertThrows(ValidationException.class,
                () -> service.ejecutar(1L, FiltroDestinatariosCommand.builder().build()));
    }

    @Test
    void testDesuscribirLlamaAlRepositorio() {
        service.ejecutar(42L);
        verify(clientePerfilRepository).desactivarComunicaciones(42L);
    }

    @Test
    void testObtenerVariablesRequeridas() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(campanaPendiente()));
        when(plantillaRepo.findById(10L)).thenReturn(Optional.of(
                plantillaConBloques("[{\"texto\":\"{{descuento}} para {{nombreCliente}}\"}]")));

        Set<String> requeridas = service.obtenerVariablesRequeridas(1L);
        assertEquals(Set.of("descuento"), requeridas);
    }

    @Test
    void testContarDestinatariosUsaElMismoFiltroQueElEnvioRealYExcluyeSinCorreo() {
        when(campanaRepo.findById(1L)).thenReturn(Optional.of(campanaPendiente()));
        when(clientePerfilRepository.buscarDestinatariosCampana(any())).thenReturn(List.of(
                cliente(1L, "cliente1@correo.com"),
                cliente(2L, "cliente2@correo.com"),
                cliente(3L, null)));

        int total = service.contarDestinatarios(1L, FiltroDestinatariosCommand.builder().soloVip(true).build());

        assertEquals(2, total);
        verify(clientePerfilRepository).buscarDestinatariosCampana(argThat(q -> Boolean.TRUE.equals(q.getSoloVip())));
    }

    @Test
    void testContarDestinatariosLanzaExcepcionSiLaCampanaNoExiste() {
        when(campanaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.playzone.pems.shared.exception.ResourceNotFoundException.class,
                () -> service.contarDestinatarios(99L, FiltroDestinatariosCommand.builder().build()));
        verify(clientePerfilRepository, never()).buscarDestinatariosCampana(any());
    }
}
