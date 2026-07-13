package com.playzone.pems.application.usuario.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ActualizarClientePerfilCommand {

    private String    nombres;
    private String    apellidoPaterno;
    private String    apellidoMaterno;
    private String    telefono;
    private String    numeroDocumento;
    private String    correo;
    private LocalDate fechaNacimiento;
    private Boolean   aceptaComunicaciones;
    private String    fotoPerfilPath;
    private boolean   actualizarFotoPerfil;
}
