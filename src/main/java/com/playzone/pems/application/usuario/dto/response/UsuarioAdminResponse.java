package com.playzone.pems.application.usuario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class UsuarioAdminResponse {
    private Long           id;
    private UUID           usuarioId;
    private String         nombre;
    private String         correo;
    private String         rol;
    private String         telefono;
    private Long           idSede;
    private String         sedeNombre;
    private boolean        activo;
    private boolean        debeCambiarContrasena;
    private Integer        intentosFallidos;
    private OffsetDateTime bloqueadoHasta;
    private OffsetDateTime ultimoAcceso;
    private OffsetDateTime fechaCreacion;
    private String         fotoPerfilUrl;
}
