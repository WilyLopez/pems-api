package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.command.VentaPagoItem;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VentaEventoWriter {

    private final VentaRepository      ventaRepository;
    private final VentaPagoRepository  ventaPagoRepository;
    private final EnrutadorCajaService enrutadorCajaService;

    public Venta crearVenta(EventoPrivado evento, String tipo, BigDecimal monto, UUID idUsuario) {
        return ventaRepository.save(Venta.builder()
                .idSede(evento.getIdSede())
                .clienteId(evento.getIdCliente())
                .eventoId(evento.getId())
                .tipo(tipo)
                .canalCodigo("MOSTRADOR")
                .subtotal(monto)
                .descuento(BigDecimal.ZERO)
                .total(monto)
                .efectivoRecibido(BigDecimal.ZERO)
                .vuelto(BigDecimal.ZERO)
                .actaFirmada(false)
                .esAnticipada(false)
                .createdBy(idUsuario)
                .build());
    }

    public void registrarPagos(Long ventaId, List<VentaPagoItem> pagos, UUID idUsuario) {
        pagos.forEach(p -> {
            ventaPagoRepository.save(VentaPago.builder()
                    .ventaId(ventaId)
                    .medioPagoCodigo(p.getMedioPagoCodigo())
                    .monto(p.getMonto())
                    .esValidado(true)
                    .validadoPor(idUsuario)
                    .validadoAt(OffsetDateTime.now())
                    .build());
            enrutadorCajaService.registrarIngresoEfectivoAdministrativo(
                    idUsuario, p.getMedioPagoCodigo(), p.getMonto(),
                    "Cobro evento venta #" + ventaId, ventaId);
        });
    }
}
