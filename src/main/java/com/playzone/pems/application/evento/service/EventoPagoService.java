package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.evento.dto.command.RegistrarPagoCuotaCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarSaldoCommand;
import com.playzone.pems.application.evento.dto.command.VentaPagoItem;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.port.in.RegistrarPagoCuotaUseCase;
import com.playzone.pems.application.evento.port.in.RegistrarSaldoUseCase;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventoPagoService implements RegistrarSaldoUseCase, RegistrarPagoCuotaUseCase {

    private final EventoPrivadoRepository  eventoRepository;
    private final EventoCuotaRepository    cuotaRepository;
    private final SedeScopeValidator       sedeScope;
    private final VentaPagoRepository      ventaPagoRepository;
    private final EnrutadorCajaService     enrutadorCajaService;
    private final CrearNotificacionPort    crearNotificacionPort;
    private final ObjectMapper             objectMapper;
    private final EventoPrivadoQueryMapper mapper;
    private final VentaEventoWriter        ventaWriter;

    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(RegistrarPagoCuotaCommand command) {
        EventoCuota cuota = cuotaRepository.findByIdForUpdate(command.getIdCuota())
                .orElseThrow(() -> new ResourceNotFoundException("EventoCuota", command.getIdCuota()));

        if (!cuota.esPendiente()) {
            throw new ValidationException("La cuota " + cuota.getNumeroCuota() + " ya está en estado " + cuota.getEstado() + ".");
        }

        BigDecimal totalPago = command.getPagos().stream()
                .map(VentaPagoItem::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPago.compareTo(cuota.getMonto()) != 0) {
            throw new ValidationException("El total de los pagos (" + totalPago +
                    ") no coincide con el monto de la cuota (" + cuota.getMonto() + ").");
        }

        EventoPrivado evento = eventoRepository.findByIdForUpdate(cuota.getEventoId())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", cuota.getEventoId()));
        sedeScope.validarAcceso(evento.getIdSede());

        Venta ventaSaldo = ventaWriter.crearVenta(evento, "SALDO_EVENTO", totalPago, command.getIdUsuario());
        ventaWriter.registrarPagos(ventaSaldo.getId(), command.getPagos(), command.getIdUsuario());

        cuotaRepository.save(cuota.toBuilder()
                .estado(EstadoCuota.PAGADO)
                .ventaId(ventaSaldo.getId())
                .build());

        BigDecimal nuevoAdelanto = evento.getMontoAdelanto().add(totalPago);
        EventoPrivado actualizado = evento.toBuilder().montoAdelanto(nuevoAdelanto).build();
        EventoPrivado guardado = eventoRepository.save(actualizado);

        ClientePerfil cliente = mapper.obtenerCliente(guardado.getIdCliente());
        enviarNotificacionAbono(guardado, cliente, totalPago);

        return mapper.toQuery(guardado, cliente, mapper.obtenerTurno(guardado.getIdTurno()), true);
    }


    @Override
    @Transactional
    public EventoPrivadoQuery registrarSaldo(RegistrarSaldoCommand command) {
        EventoPrivado evento = eventoRepository.findByIdForUpdate(command.getIdEvento())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", command.getIdEvento()));
        sedeScope.validarAcceso(evento.getIdSede());

        if (command.getMonto() == null || command.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("monto", "El monto del saldo debe ser mayor a 0.");
        }
        if (evento.getPrecioContrato() == null) {
            throw new ValidationException("El evento aun no tiene un precio de contrato definido.");
        }
        BigDecimal saldoPendiente = evento.calcularMontoSaldo();
        if (command.getMonto().compareTo(saldoPendiente) > 0) {
            throw new ValidationException("monto",
                    "El monto (" + command.getMonto() + ") supera el saldo pendiente (" + saldoPendiente + ").");
        }

        Venta ventaSaldo = ventaWriter.crearVenta(evento, "SALDO_EVENTO", command.getMonto(), command.getIdUsuario());
        ventaPagoRepository.save(VentaPago.builder()
                .ventaId(ventaSaldo.getId())
                .medioPagoCodigo(command.getMedioPago())
                .monto(command.getMonto())
                .esValidado(true)
                .validadoPor(command.getIdUsuario())
                .validadoAt(OffsetDateTime.now())
                .build());
        enrutadorCajaService.registrarIngresoEfectivoAdministrativo(
                command.getIdUsuario(), command.getMedioPago(), command.getMonto(),
                "Saldo evento #" + evento.getId(), ventaSaldo.getId());

        BigDecimal nuevoAdelanto = evento.getMontoAdelanto().add(command.getMonto());
        EventoPrivado guardado = eventoRepository.save(evento.toBuilder().montoAdelanto(nuevoAdelanto).build());

        ClientePerfil cliente = mapper.obtenerCliente(guardado.getIdCliente());
        enviarNotificacionAbono(guardado, cliente, command.getMonto());

        return mapper.toQuery(guardado, cliente, mapper.obtenerTurno(guardado.getIdTurno()), false);
    }

    private void enviarNotificacionAbono(EventoPrivado guardado, ClientePerfil cliente, BigDecimal monto) {
        BigDecimal saldo = guardado.calcularMontoSaldo() != null ? guardado.calcularMontoSaldo() : BigDecimal.ZERO;

        if (guardado.getIdUsuarioGestor() != null) {
            crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                    .tipoCodigo("EVENTO_SALDO_RECIBIDO")
                    .destinatarioUsuarioId(guardado.getIdUsuarioGestor())
                    .entidadTipo("evento_privado")
                    .entidadId(guardado.getId())
                    .datosExtra(Map.of(
                            "monto",   monto.toPlainString(),
                            "cliente", cliente.nombreCompleto(),
                            "evento",  guardado.getNombreTipoEvento() != null ? guardado.getNombreTipoEvento() : guardado.getTipoEvento(),
                            "saldo",   saldo.toPlainString()))
                    .build());
        }

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(Map.of("montoAbonado", monto.toPlainString()));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el monto del abono.", e);
        }

        crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("EVENTO_ABONO_RECIBIDO")
                .destinatarioClienteId(guardado.getIdCliente())
                .entidadTipo("evento_privado")
                .entidadId(guardado.getId())
                .datosExtra(Map.of(
                        "monto", monto.toPlainString(),
                        "fecha", guardado.getFechaEvento().toString(),
                        "saldo", saldo.toPlainString()))
                .metadata(metadataJson)
                .build());
    }
}
