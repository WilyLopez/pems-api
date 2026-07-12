package com.playzone.pems.infrastructure.external.correo.renderizador;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RenderizadorCorreoRegistry {

    private final Map<String, RenderizadorCorreoTransaccional> renderizadores;

    public RenderizadorCorreoRegistry(List<RenderizadorCorreoTransaccional> renderizadores) {
        this.renderizadores = renderizadores.stream()
                .collect(Collectors.toMap(RenderizadorCorreoTransaccional::tipoCodigo, Function.identity()));
    }

    public Optional<RenderizadorCorreoTransaccional> resolver(String tipoCodigo) {
        return Optional.ofNullable(renderizadores.get(tipoCodigo));
    }
}
