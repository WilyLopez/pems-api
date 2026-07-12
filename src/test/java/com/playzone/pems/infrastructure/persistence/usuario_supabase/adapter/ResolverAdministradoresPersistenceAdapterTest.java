package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.StaffPerfilJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolverAdministradoresPersistenceAdapterTest {

    @Mock private StaffPerfilJpaRepository jpa;

    private ResolverAdministradoresPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ResolverAdministradoresPersistenceAdapter(jpa);
    }

    @Test
    void testDelegaEnLaConsultaNativaDeAdministradoresActivos() {
        UUID idAdmin = UUID.randomUUID();
        when(jpa.obtenerAdministradoresActivos()).thenReturn(List.of(idAdmin));

        List<UUID> resultado = adapter.obtenerIdsAdministradoresActivos();

        assertEquals(List.of(idAdmin), resultado);
    }
}
