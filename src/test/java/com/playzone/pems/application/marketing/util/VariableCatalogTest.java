package com.playzone.pems.application.marketing.util;

import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VariableCatalogTest {

    @Test
    void testVariablesRequeridasDetectaVariablesSinFuenteAutomatica() {
        String contenido = "[{\"texto\":\"Aprovecha {{promocion}} con {{descuento}} para {{nombreCliente}}\"}]";
        Set<String> requeridas = VariableCatalog.variablesRequeridas(contenido);
        assertEquals(Set.of("promocion", "descuento"), requeridas);
    }

    @Test
    void testVariablesRequeridasVaciaSiSoloUsaAutomaticas() {
        String contenido = "[{\"texto\":\"Hola {{nombreCliente}}, en {{mes}} de {{anio}}\"}]";
        assertTrue(VariableCatalog.variablesRequeridas(contenido).isEmpty());
    }

    @Test
    void testVariablesRequeridasIgnoraVariablesNoCatalogadas() {
        String contenido = "[{\"texto\":\"{{variableInventada}}\"}]";
        assertTrue(VariableCatalog.variablesRequeridas(contenido).isEmpty());
    }

    @Test
    void testValidarParaMarketingRechazaVariableBloqueada() {
        assertThrows(ValidationException.class, () ->
                VariableCatalog.validarParaMarketing("{{codigoReserva}}"));
    }

    @Test
    void testValidarParaMarketingAceptaVariablePermitida() {
        assertDoesNotThrow(() -> VariableCatalog.validarParaMarketing("{{promocion}} {{nombreCliente}}"));
    }
}
