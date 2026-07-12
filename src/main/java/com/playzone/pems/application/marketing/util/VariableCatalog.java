package com.playzone.pems.application.marketing.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VariableCatalog {

    private VariableCatalog() {}

    public static final Set<String> MARKETING_PERMITIDAS = Set.of(
            "nombreCliente",
            "nombreNegocio",
            "promocion",
            "descuento",
            "fechaVigencia",
            "urlReserva",
            "mes",
            "anio",
            "nombreNino",
            "ultimaVisita"
    );

    public static final Set<String> AUTO_RESUELTAS = Set.of(
            "nombreCliente",
            "nombreNegocio",
            "ultimaVisita",
            "mes",
            "anio"
    );

    public static final Set<String> SISTEMA_BLOQUEADAS = Set.of(
            "qrUrl",
            "codigoTicket",
            "montoPago",
            "reembolso",
            "codigoReserva",
            "fechaEvento",
            "cantidadNinos",
            "referencia",
            "idCliente",
            "idReserva",
            "claveTemporal",
            "tokenAcceso",
            "passwordHash",
            "sedeId",
            "usuarioId"
    );

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public static Set<String> extraerVariables(String texto) {
        if (texto == null || texto.isBlank()) return Set.of();
        Matcher m = VAR_PATTERN.matcher(texto);
        Set<String> vars = new java.util.HashSet<>();
        while (m.find()) vars.add(m.group(1));
        return vars;
    }

    public static void validarParaMarketing(String contenido) {
        Set<String> usadas = extraerVariables(contenido);
        for (String v : usadas) {
            if (SISTEMA_BLOQUEADAS.contains(v)) {
                throw new com.playzone.pems.shared.exception.ValidationException(
                        "variables",
                        "La variable '{{" + v + "}}' está reservada para el sistema y no puede usarse en correos de marketing."
                );
            }
        }
    }

    public static Set<String> variablesRequeridas(String contenido) {
        Set<String> requeridas = new HashSet<>(extraerVariables(contenido));
        requeridas.retainAll(MARKETING_PERMITIDAS);
        requeridas.removeAll(AUTO_RESUELTAS);
        return requeridas;
    }
}
