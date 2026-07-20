package com.playzone.pems.domain.evento.model.enums;

import java.util.Arrays;

public enum EstadoCuota {

    PENDIENTE("PENDIENTE", "Cuota pendiente de pago"),
    PAGADO("PAGADO", "Cuota pagada"),
    VENCIDO("VENCIDO", "Cuota vencida sin pago");

    private final String codigo;
    private final String descripcion;

    EstadoCuota(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    public static EstadoCuota desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(e -> e.codigo.equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Estado de cuota inválido: '" + codigo + "'"));
    }
}
