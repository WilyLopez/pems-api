package com.playzone.pems.application.usuario.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistrarUsuarioAdminCommand {
    private String nombre;
    private String correo;
    private String rolCodigo;
    private Long   sedeId;
    private String telefono;
}
