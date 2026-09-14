package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.MovimientoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrutadorCajaService {

    private static final String MEDIO_EFECTIVO = "EFECTIVO";
    private static final String MSG_SIN_CAJA =
            "No hay una caja abierta en esta sede. Abre una caja antes de cobrar en efectivo.";
    private static final String MSG_SIN_CAJA_INGRESO =
            "No hay una caja abierta en esta sede. Abre una caja antes de registrar ingresos en efectivo.";
    private static final String MSG_SIN_CAJA_EGRESO =
            "No hay una caja abierta en esta sede. Abre una caja antes de registrar egresos en efectivo.";
    private static final String MSG_CAJA_CERRADA_EN_OPERACION =
            "La caja fue cerrada durante la operacion. Abre una caja e intenta nuevamente.";

    private final SesionCajaRepository     sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;

    public void registrarIngresoEfectivo(Long idSede, UUID cobrador, String medioPago, BigDecimal monto,
                                         String concepto, Long ventaId) {
        enrutar(idSede, TipoMovimientoCaja.INGRESO, cobrador, medioPago, monto, concepto,
                ventaId, null, null, MSG_SIN_CAJA);
    }

    public void registrarIngresoEfectivoAdministrativo(Long idSede, UUID gestor, String medioPago, BigDecimal monto,
                                                       String concepto, Long ventaId) {
        enrutar(idSede, TipoMovimientoCaja.INGRESO, gestor, medioPago, monto, concepto,
                ventaId, null, null, MSG_SIN_CAJA);
    }

    public void registrarIngresoManualEfectivo(Long idSede, UUID gestor, String medioPago, BigDecimal monto,
                                               String concepto, Long registroIngresoId) {
        enrutar(idSede, TipoMovimientoCaja.INGRESO, gestor, medioPago, monto, concepto,
                null, registroIngresoId, null, MSG_SIN_CAJA_INGRESO);
    }

    public void registrarEgresoManualEfectivo(Long idSede, UUID gestor, String medioPago, BigDecimal monto,
                                              String concepto, Long registroEgresoId) {
        enrutar(idSede, TipoMovimientoCaja.EGRESO, gestor, medioPago, monto, concepto,
                null, null, registroEgresoId, MSG_SIN_CAJA_EGRESO);
    }

    private void enrutar(Long idSede, TipoMovimientoCaja tipo, UUID usuario, String medioPago, BigDecimal monto,
                         String concepto, Long ventaId, Long registroIngresoId, Long registroEgresoId,
                         String mensajeSinSesion) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (!MEDIO_EFECTIVO.equals(medioPago)) {
            return;
        }
        if (usuario == null || idSede == null) {
            throw new ValidationException(mensajeSinSesion);
        }
        SesionCaja sesion = sesionCajaRepository.findAbiertaBySede(idSede)
                .orElseThrow(() -> new ValidationException(mensajeSinSesion));
        movimientoCajaRepository.save(MovimientoCaja.builder()
                .idSesionCaja(sesion.getId())
                .tipo(tipo)
                .concepto(concepto)
                .monto(monto)
                .medioPago(medioPago)
                .idVenta(ventaId)
                .idRegistroIngreso(registroIngresoId)
                .idRegistroEgreso(registroEgresoId)
                .esManual(false)
                .idUsuarioRegistra(usuario)
                .build());
        int actualizados = tipo == TipoMovimientoCaja.INGRESO
                ? sesionCajaRepository.incrementarIngresosSiAbierta(sesion.getId(), monto)
                : sesionCajaRepository.incrementarEgresosSiAbierta(sesion.getId(), monto);
        if (actualizados == 0) {
            throw new ValidationException(MSG_CAJA_CERRADA_EN_OPERACION);
        }
    }
}
