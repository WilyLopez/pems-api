package com.playzone.pems.domain.evento.model.enums;

import java.util.Arrays;

public enum ModalidadPago {

    AL_CONTADO("AL_CONTADO", "Pago al contado"),
    CUOTAS("CUOTAS", "Pago en cuotas");

    private final String codigo;
    private final String descripcion;

    ModalidadPago(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    public static ModalidadPago desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(e -> e.codigo.equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Modalidad de pago inválida: '" + codigo + "'"));
    }
}
