package com.playzone.pems.application.evento.port.in;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ConsultarReservasUseCase {

    Page<ReservaPublicaQuery> consultarPorCliente(Long idCliente, Pageable pageable);

    Page<ReservaPublicaQuery> consultarPorSedeYFecha(Long idSede, LocalDate fecha, Pageable pageable);

    Page<ReservaPublicaQuery> consultarPorSedeYEstado(Long idSede, String estado, Pageable pageable);

    ReservaPublicaQuery consultarPorId(Long idReserva);
}
