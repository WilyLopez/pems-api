package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.StaffPerfilJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResolverAdministradoresPersistenceAdapter implements ResolverAdministradoresPort {

    private final StaffPerfilJpaRepository jpa;

    @Override
    public List<UUID> obtenerIdsAdministradoresActivos() {
        return jpa.obtenerAdministradoresActivos();
    }
}
