package com.playzone.pems.interfaces.rest.usuario.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientePerfilResponse {

    private Long           id;
    private String         tipoDocumentoCodigo;
    private String         numeroDocumento;
    private String         nombres;
    private String         apellidoPaterno;
    private String         apellidoMaterno;
    private String         nombreCompleto;
    private String         correo;
    private String         telefono;
    private LocalDate      fechaNacimiento;
    private boolean        activo;
    private boolean        esVip;
    private BigDecimal     descuentoVip;
    private int            contadorVisitas;
    private OffsetDateTime ultimaVisitaAt;
    private BigDecimal     totalGastado;
    private String         segmentoCodigo;
    private String         origen;
    private boolean        aceptaComunicaciones;
    private OffsetDateTime creadoEn;
    private String         fotoPerfilPath;
}
