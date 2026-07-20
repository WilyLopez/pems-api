package com.playzone.pems.domain.evento.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum EstadoEventoPrivado {

    SOLICITADA(
            "SOLICITADA",
            "Solicitud recibida, pendiente de contacto"
    ),
    CONFIRMADA(
            "CONFIRMADA",
            "Contrato firmado, evento agendado"
    ),
    EN_CURSO(
            "EN_CURSO",
            "Evento en curso"
    ),
    COMPLETADA(
            "COMPLETADA",
            "Evento realizado exitosamente"
    ),
    CANCELADA(
            "CANCELADA",
            "Evento cancelado con justificación"
    );

    private final String codigo;
    private final String descripcion;

    private static final Set<EstadoEventoPrivado> ESTADOS_TERMINALES =
            Set.of(COMPLETADA, CANCELADA);

    private static final Set<EstadoEventoPrivado> CANCELABLES =
            Set.of(SOLICITADA, CONFIRMADA);

    public boolean esTerminal() {
        return ESTADOS_TERMINALES.contains(this);
    }

    public boolean esCancelable() {
        return CANCELABLES.contains(this);
    }

    public boolean bloqueaDisponibilidadPublica() {
        return this == SOLICITADA || this == CONFIRMADA || this == EN_CURSO;
    }

    public boolean requiereMotivoCancelacion() {
        return this == CANCELADA;
    }

    public static EstadoEventoPrivado desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(e -> e.codigo.equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Estado de evento privado inválido: '" + codigo + "'"));
    }
}