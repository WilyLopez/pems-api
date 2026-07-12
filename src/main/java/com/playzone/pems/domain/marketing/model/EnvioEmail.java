package com.playzone.pems.domain.marketing.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EnvioEmail {

    private Long    id;
    private Long    idCampanaEmail;
    private Long    idCliente;
    private String  destinatario;
    private String  asunto;
    private String  cuerpoHtml;
    private String  estado;
    private int     intentos;
    private Instant fechaEnvio;
    private String  mensajeError;
    private String  proveedorMensajeId;
    private Instant fechaCreacion;

    public boolean puedeReintentar() {
        return "ERROR".equals(estado) && intentos < 3;
    }
}
