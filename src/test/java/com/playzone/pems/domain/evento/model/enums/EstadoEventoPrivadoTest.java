package com.playzone.pems.domain.evento.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoEventoPrivadoTest {

    @Test
    void testDesdeCodigoResuelveEnCurso() {
        assertEquals(EstadoEventoPrivado.EN_CURSO, EstadoEventoPrivado.desdeCodigo("EN_CURSO"));
    }

    @Test
    void testEnCursoNoEsTerminal() {
        assertFalse(EstadoEventoPrivado.EN_CURSO.esTerminal());
    }

    @Test
    void testEnCursoNoEsCancelable() {
        assertFalse(EstadoEventoPrivado.EN_CURSO.esCancelable());
    }

    @Test
    void testEnCursoBloqueaDisponibilidadPublica() {
        assertTrue(EstadoEventoPrivado.EN_CURSO.bloqueaDisponibilidadPublica());
    }

    @Test
    void testEnCursoNoRequiereMotivoCancelacion() {
        assertFalse(EstadoEventoPrivado.EN_CURSO.requiereMotivoCancelacion());
    }
}
