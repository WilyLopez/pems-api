package com.playzone.pems.domain.evento.model.enums;

import java.util.Arrays;
import java.util.Set;

public enum EstadoReservaPublica {

    PENDIENTE(
            "PENDIENTE",
            "Reserva creada, pago aún no confirmado"
    ),
    CONFIRMADA(
            "CONFIRMADA",
            "Pago confirmado, ticket generado"
    ),
    REPROGRAMADA(
            "REPROGRAMADA",
            "Entrada reprogramada a otra fecha"
    ),
    COMPLETADA(
            "COMPLETADA",
            "Visita realizada"
    ),
    CANCELADA(
            "CANCELADA",
            "Reserva cancelada"
    ),
    VENCIDA(
            "VENCIDA",
            "No se presento dentro del plazo, cupo liberado automaticamente"
    );

    private final String codigo;
    private final String descripcion;

    EstadoReservaPublica(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    private static final Set<EstadoReservaPublica> ESTADOS_TERMINALES =
            Set.of(COMPLETADA, CANCELADA);

    private static final Set<EstadoReservaPublica> CANCELABLES =
            Set.of(PENDIENTE, CONFIRMADA);

    private static final Set<EstadoReservaPublica> REPROGRAMABLES =
            Set.of(CONFIRMADA, VENCIDA);

    public boolean esTerminal() {
        return ESTADOS_TERMINALES.contains(this);
    }

    public boolean esCancelable() {
        return CANCELABLES.contains(this);
    }

    public boolean esReprogramable() {
        return REPROGRAMABLES.contains(this);
    }

    public boolean ocupaAforo() {
        return this == CONFIRMADA || this == COMPLETADA;
    }

    public static EstadoReservaPublica desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(e -> e.codigo.equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Estado de reserva pública inválido: '" + codigo + "'"));
    }
}