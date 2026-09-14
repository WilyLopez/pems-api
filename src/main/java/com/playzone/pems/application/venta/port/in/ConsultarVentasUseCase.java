package com.playzone.pems.application.venta.port.in;

import com.playzone.pems.application.venta.dto.query.VentaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;

public interface ConsultarVentasUseCase {

    VentaQuery consultarPorId(Long idVenta);

    VentaDetalleQuery consultarDetallePorId(Long idVenta);

    Page<VentaQuery> consultarPorSedeYFechas(
            Long idSede, LocalDate desde, LocalDate hasta, String search, UUID usuarioId, Pageable pageable);
}