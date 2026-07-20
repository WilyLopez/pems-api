package com.playzone.pems.application.evento.service;

import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventoAccesoValidator {

    private final SupabaseAuthFacade supabaseAuthFacade;
    private final SedeScopeValidator sedeScope;

    public void validarAccesoAlEvento(Long idClienteRecurso, Long idSede) {
        if (supabaseAuthFacade.tieneRol("CLIENTE")) {
            Long propio = supabaseAuthFacade.clientePerfilId()
                    .orElseThrow(() -> new AccessDeniedException("Cliente sin perfil asociado."));
            if (!propio.equals(idClienteRecurso)) {
                throw new AccessDeniedException("No puedes acceder al evento de otro cliente.");
            }
        } else {
            sedeScope.validarAcceso(idSede);
        }
    }
}
