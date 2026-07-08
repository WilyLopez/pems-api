package com.playzone.pems.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component("sedeScope")
@RequiredArgsConstructor
public class SedeScopeValidator {

    private static final String MSG_SEDE_NO_PERMITIDA =
            "No tienes acceso a la sede indicada.";

    private final SupabaseAuthFacade authFacade;

    public boolean puedeAcceder(Long idSede) {
        if (idSede == null) {
            return false;
        }
        if (esAdminGlobal()) {
            return true;
        }
        return authFacade.sedeId().map(idSede::equals).orElse(false);
    }

    public void validarAcceso(Long idSede) {
        if (!puedeAcceder(idSede)) {
            throw new AccessDeniedException(MSG_SEDE_NO_PERMITIDA);
        }
    }

    public void validarAccesoFiltro(Long idSede) {
        if (idSede == null) {
            if (!esAdminGlobal()) {
                throw new AccessDeniedException(MSG_SEDE_NO_PERMITIDA);
            }
            return;
        }
        validarAcceso(idSede);
    }

    private boolean esAdminGlobal() {
        return authFacade.tieneRol("SUPERADMIN") || authFacade.tieneRol("ADMIN");
    }
}
