package com.playzone.pems.shared.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactoAdicionalValidatorTest {

    private final ContactoAdicionalValidator.ContactoAdicionalConstraintValidator validator =
            new ContactoAdicionalValidator.ContactoAdicionalConstraintValidator();

    @ParameterizedTest
    @NullAndEmptySource
    void testValoresNuloOVacioSonValidos(String valor) {
        assertTrue(validator.isValid(valor, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "987654321", "912345678", "ana@correo.com", "cliente@dominio.com.pe" })
    void testCelularQueEmpiezaCon9YCorreoConFormatoValidoSonValidos(String valor) {
        assertTrue(validator.isValid(valor, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "812345678", "12345", "correo-sin-arroba.com", "@sindominio", "987654321extra" })
    void testFormatosInvalidosSonRechazados(String valor) {
        assertFalse(validator.isValid(valor, null));
    }
}
