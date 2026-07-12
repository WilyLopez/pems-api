package com.playzone.pems.interfaces.rest.marketing;

import com.playzone.pems.application.marketing.port.in.*;
import com.playzone.pems.application.marketing.service.MarketingService;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.TokenEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingControllerUnsubscribeTest {

    @Mock private CrearPlantillaEmailUseCase crearPlantillaUseCase;
    @Mock private ListarPlantillasUseCase listarPlantillasUseCase;
    @Mock private CrearCampanaEmailUseCase crearCampanaUseCase;
    @Mock private ListarCampanasUseCase listarCampanasUseCase;
    @Mock private EnviarCampanaUseCase enviarCampanaUseCase;
    @Mock private ListarEnviosUseCase listarEnviosUseCase;
    @Mock private DesuscribirClienteUseCase desuscribirClienteUseCase;
    @Mock private MarketingService marketingService;
    @Mock private SupabaseAuthFacade supabaseAuthFacade;
    @Mock private TokenEncryptor tokenEncryptor;

    private MarketingController controller;

    @BeforeEach
    void setUp() {
        controller = new MarketingController(
                crearPlantillaUseCase, listarPlantillasUseCase, crearCampanaUseCase,
                listarCampanasUseCase, enviarCampanaUseCase, listarEnviosUseCase,
                desuscribirClienteUseCase, marketingService, supabaseAuthFacade, tokenEncryptor);
    }

    @Test
    void testUnsubscribeDecodificaTokenYDesuscribeAlCliente() {
        when(tokenEncryptor.decrypt("token-valido")).thenReturn("42");

        ResponseEntity<String> respuesta = controller.unsubscribe("token-valido");

        verify(desuscribirClienteUseCase).ejecutar(42L);
        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().contains("diste de baja"));
    }

    @Test
    void testUnsubscribeConTokenInvalidoLanzaValidationException() {
        when(tokenEncryptor.decrypt("token-corrupto")).thenThrow(new RuntimeException("bad padding"));

        assertThrows(ValidationException.class, () -> controller.unsubscribe("token-corrupto"));
        verify(desuscribirClienteUseCase, never()).ejecutar(anyLong());
    }
}
