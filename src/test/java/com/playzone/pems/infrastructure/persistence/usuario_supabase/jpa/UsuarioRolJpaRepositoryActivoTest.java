package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.UsuarioRolEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRolJpaRepositoryActivoTest {

    @Autowired
    private UsuarioRolJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void crearTablasDeCatalogo() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS usuario_rol (usuario_id UUID NOT NULL, rol_codigo VARCHAR(50) NOT NULL, asignado_at TIMESTAMP NOT NULL, PRIMARY KEY (usuario_id, rol_codigo))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rol (codigo VARCHAR(50) PRIMARY KEY, activo BOOLEAN NOT NULL DEFAULT TRUE)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS permiso (codigo VARCHAR(100) PRIMARY KEY, activo BOOLEAN NOT NULL DEFAULT TRUE)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rol_permiso (rol_codigo VARCHAR(50) NOT NULL, permiso_codigo VARCHAR(100) NOT NULL, PRIMARY KEY (rol_codigo, permiso_codigo))");
    }

    private void asignarRol(UUID usuarioId, String rolCodigo) {
        UsuarioRolEntity ur = new UsuarioRolEntity();
        ur.setUsuarioId(usuarioId);
        ur.setRolCodigo(rolCodigo);
        ur.setAsignadoAt(OffsetDateTime.now());
        repository.save(ur);
    }

    @Test
    void findPermisoCodigosByUsuarioId_excluyePermisoDesactivadoEnCatalogo() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO rol (codigo, activo) VALUES ('CAJERO', TRUE)");
        jdbcTemplate.update("INSERT INTO permiso (codigo, activo) VALUES ('pos.vender', TRUE)");
        jdbcTemplate.update("INSERT INTO permiso (codigo, activo) VALUES ('caja.abrir', FALSE)");
        jdbcTemplate.update("INSERT INTO rol_permiso (rol_codigo, permiso_codigo) VALUES ('CAJERO', 'pos.vender')");
        jdbcTemplate.update("INSERT INTO rol_permiso (rol_codigo, permiso_codigo) VALUES ('CAJERO', 'caja.abrir')");
        asignarRol(usuarioId, "CAJERO");

        List<String> permisos = repository.findPermisoCodigosByUsuarioId(usuarioId);

        assertThat(permisos).containsExactly("pos.vender");
    }

    @Test
    void findRolCodigosByUsuarioId_excluyeRolDesactivadoEnCatalogo() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO rol (codigo, activo) VALUES ('CAJERO', FALSE)");
        asignarRol(usuarioId, "CAJERO");

        List<String> roles = repository.findRolCodigosByUsuarioId(usuarioId);

        assertThat(roles).isEmpty();
    }

    @Test
    void findRolCodigosByUsuarioId_incluyeRolActivo() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO rol (codigo, activo) VALUES ('ADMIN', TRUE)");
        asignarRol(usuarioId, "ADMIN");

        List<String> roles = repository.findRolCodigosByUsuarioId(usuarioId);

        assertThat(roles).containsExactly("ADMIN");
    }
}
