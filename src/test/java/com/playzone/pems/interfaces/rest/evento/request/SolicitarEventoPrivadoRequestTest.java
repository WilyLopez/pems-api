package com.playzone.pems.interfaces.rest.evento.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitarEventoPrivadoRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private SolicitarEventoPrivadoRequest requestBase() {
        SolicitarEventoPrivadoRequest request = new SolicitarEventoPrivadoRequest();
        ReflectionTestUtils.setField(request, "idTurno", 1L);
        ReflectionTestUtils.setField(request, "fechaEvento", LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(request, "tipoEvento", "CUMPLEANOS");
        return request;
    }

    private boolean tieneViolacionEn(
            Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones, String propiedad) {
        return violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(propiedad));
    }

    @Test
    void testPresupuestoNegativoEsRechazado() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "presupuestoEstimado", new BigDecimal("-1"));

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertTrue(tieneViolacionEn(violaciones, "presupuestoEstimado"));
    }

    @Test
    void testPresupuestoCeroEsValido() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "presupuestoEstimado", BigDecimal.ZERO);

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertFalse(tieneViolacionEn(violaciones, "presupuestoEstimado"));
    }

    @Test
    void testNombreNinoConNumerosEsRechazado() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "nombreNino", "Juan123");

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertTrue(tieneViolacionEn(violaciones, "nombreNino"));
    }

    @Test
    void testNombreNinoSoloLetrasEsValido() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "nombreNino", "María José");

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertFalse(tieneViolacionEn(violaciones, "nombreNino"));
    }

    @Test
    void testTipoEventoDe121CaracteresEsRechazado() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "tipoEvento", "A".repeat(121));

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertTrue(tieneViolacionEn(violaciones, "tipoEvento"));
    }

    @Test
    void testTipoEventoDe120CaracteresEsValido() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "tipoEvento", "A".repeat(120));

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertFalse(tieneViolacionEn(violaciones, "tipoEvento"));
    }

    @Test
    void testExtraLibreDeMasDe500CaracteresEsRechazado() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "extrasLibres", List.of("A".repeat(501)));

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().startsWith("extrasLibres")));
    }

    @Test
    void testExtraLibreDe500CaracteresEsValido() {
        SolicitarEventoPrivadoRequest request = requestBase();
        ReflectionTestUtils.setField(request, "extrasLibres", List.of("A".repeat(500)));

        Set<ConstraintViolation<SolicitarEventoPrivadoRequest>> violaciones = validator.validate(request);

        assertFalse(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().startsWith("extrasLibres")));
    }
}
