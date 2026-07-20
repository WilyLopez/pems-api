package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventoCuotaGenerator {

    private final EventoCuotaRepository cuotaRepository;

    public void validarParametrosCuotas(ConfirmarEventoCommand command) {
        if (command.getNumeroCuotas() == null || command.getNumeroCuotas() < 2) {
            throw new ValidationException("numeroCuotas",
                    "Se requiere al menos 2 cuotas para la modalidad CUOTAS.");
        }
        if (command.getFechaLimitePago() == null) {
            throw new ValidationException("fechaLimitePago",
                    "Se requiere fecha límite de pago para la modalidad CUOTAS.");
        }
        if (!command.getFechaLimitePago().isAfter(LocalDate.now())) {
            throw new ValidationException("fechaLimitePago",
                    "La fecha límite de pago debe ser futura.");
        }
        BigDecimal adelanto = command.getMontoAdelanto() != null ? command.getMontoAdelanto() : BigDecimal.ZERO;
        if (adelanto.compareTo(command.getPrecioTotal()) >= 0) {
            throw new ValidationException("montoAdelanto",
                    "El adelanto no puede cubrir el 100% del precio en modalidad CUOTAS.");
        }
    }

    public void crearCuotas(EventoPrivado evento, BigDecimal adelanto,
                             ConfirmarEventoCommand command, Venta ventaAdelanto) {
        int n = command.getNumeroCuotas();
        LocalDate hoy = LocalDate.now();
        LocalDate limite = command.getFechaLimitePago();
        boolean hayAdelanto = adelanto.compareTo(BigDecimal.ZERO) > 0;

        List<EventoCuota> cuotas = new ArrayList<>();
        if (hayAdelanto) {
            cuotas.add(EventoCuota.builder()
                    .eventoId(evento.getId())
                    .numeroCuota(1)
                    .monto(adelanto)
                    .fechaVencimiento(hoy)
                    .estado(ventaAdelanto != null ? EstadoCuota.PAGADO : EstadoCuota.PENDIENTE)
                    .ventaId(ventaAdelanto != null ? ventaAdelanto.getId() : null)
                    .build());
        }

        BigDecimal restante = command.getPrecioTotal().subtract(adelanto);
        int primerNumeroPendiente = hayAdelanto ? 2 : 1;
        int cantidadPendientes = hayAdelanto ? n - 1 : n;
        BigDecimal montoPorCuota = restante.divide(BigDecimal.valueOf(cantidadPendientes), 2, RoundingMode.FLOOR);
        BigDecimal acumulado = montoPorCuota.multiply(BigDecimal.valueOf(cantidadPendientes - 1));
        BigDecimal montoUltima = restante.subtract(acumulado);

        long diasTotal = ChronoUnit.DAYS.between(hoy, limite);

        for (int i = 0; i < cantidadPendientes; i++) {
            boolean esUltima = i == cantidadPendientes - 1;
            long diasOffset = cantidadPendientes == 1 ? diasTotal
                    : diasTotal * (i + 1) / cantidadPendientes;
            BigDecimal monto = esUltima ? montoUltima : montoPorCuota;

            cuotas.add(EventoCuota.builder()
                    .eventoId(evento.getId())
                    .numeroCuota(primerNumeroPendiente + i)
                    .monto(monto)
                    .fechaVencimiento(hoy.plusDays(diasOffset))
                    .estado(EstadoCuota.PENDIENTE)
                    .build());
        }

        cuotaRepository.saveAll(cuotas);
    }
}
