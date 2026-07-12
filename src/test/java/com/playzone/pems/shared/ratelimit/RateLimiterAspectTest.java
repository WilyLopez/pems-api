package com.playzone.pems.shared.ratelimit;

import com.playzone.pems.shared.exception.TooManyRequestsException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterAspectTest {

    @Mock private JoinPoint joinPoint;
    @Mock private Signature signature;

    private final RateLimiterAspect aspect = new RateLimiterAspect();

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testPermiteHastaElLimiteConfigurado() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("MensajeContactoController.registrar()");
        RateLimited rateLimited = anotacionConLimite(3, 60);

        assertDoesNotThrow(() -> aspect.verificarLimite(joinPoint, rateLimited));
        assertDoesNotThrow(() -> aspect.verificarLimite(joinPoint, rateLimited));
        assertDoesNotThrow(() -> aspect.verificarLimite(joinPoint, rateLimited));
    }

    @Test
    void testBloqueaAlSuperarElLimiteConfigurado() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("MensajeContactoController.registrar()");
        RateLimited rateLimited = anotacionConLimite(3, 60);

        aspect.verificarLimite(joinPoint, rateLimited);
        aspect.verificarLimite(joinPoint, rateLimited);
        aspect.verificarLimite(joinPoint, rateLimited);

        assertThrows(TooManyRequestsException.class,
                () -> aspect.verificarLimite(joinPoint, rateLimited));
    }

    private RateLimited anotacionConLimite(int requests, int durationInSeconds) {
        return new RateLimited() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimited.class;
            }

            @Override
            public int requests() {
                return requests;
            }

            @Override
            public int durationInSeconds() {
                return durationInSeconds;
            }
        };
    }
}
