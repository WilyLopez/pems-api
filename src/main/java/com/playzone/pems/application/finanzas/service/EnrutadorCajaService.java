package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
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
            "No tienes una caja abierta. Abre tu caja antes de cobrar en efectivo.";
    private static final String MSG_SIN_CAJA_ADMINISTRATIVA =
            "Necesitas tu Caja Administrativa abierta para registrar cobros en efectivo.";
    private static final String MSG_SIN_CAJA_ADMINISTRATIVA_INGRESO =
            "Necesitas tu Caja Administrativa abierta para registrar ingresos en efectivo.";
    private static final String MSG_SIN_CAJA_ADMINISTRATIVA_EGRESO =
            "Necesitas tu Caja Administrativa abierta para registrar egresos en efectivo.";
    private static final String MSG_CAJA_CERRADA_EN_OPERACION =
            "Tu caja fue cerrada durante la operacion. Abre tu caja e intenta nuevamente.";

    private final SesionCajaRepository     sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;

    public void registrarIngresoEfectivo(UUID cobrador, String medioPago, BigDecimal monto,
                                         String concepto, Long ventaId) {
        enrutar(TipoMovimientoCaja.INGRESO, cobrador, medioPago, monto, concepto,
                ventaId, null, null, null, MSG_SIN_CAJA);
    }

    public void registrarIngresoEfectivoAdministrativo(UUID gestor, String medioPago, BigDecimal monto,
                                                       String concepto, Long ventaId) {
        enrutar(TipoMovimientoCaja.INGRESO, gestor, medioPago, monto, concepto,
                ventaId, null, null, TipoSesionCaja.ADMINISTRATIVA, MSG_SIN_CAJA_ADMINISTRATIVA);
    }

    public void registrarIngresoManualEfectivo(UUID gestor, String medioPago, BigDecimal monto,
                                               String concepto, Long registroIngresoId) {
        enrutar(TipoMovimientoCaja.INGRESO, gestor, medioPago, monto, concepto,
                null, registroIngresoId, null,
                TipoSesionCaja.ADMINISTRATIVA, MSG_SIN_CAJA_ADMINISTRATIVA_INGRESO);
    }

    public void registrarEgresoManualEfectivo(UUID gestor, String medioPago, BigDecimal monto,
                                              String concepto, Long registroEgresoId) {
        enrutar(TipoMovimientoCaja.EGRESO, gestor, medioPago, monto, concepto,
                null, null, registroEgresoId,
                TipoSesionCaja.ADMINISTRATIVA, MSG_SIN_CAJA_ADMINISTRATIVA_EGRESO);
    }

    private void enrutar(TipoMovimientoCaja tipo, UUID usuario, String medioPago, BigDecimal monto,
                         String concepto, Long ventaId, Long registroIngresoId, Long registroEgresoId,
                         TipoSesionCaja tipoRequerido, String mensajeSinSesion) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (!MEDIO_EFECTIVO.equals(medioPago)) {
            return;
        }
        if (usuario == null) {
            throw new ValidationException(mensajeSinSesion);
        }
        SesionCaja sesion = sesionCajaRepository.findAbiertaByUsuario(usuario)
                .orElseThrow(() -> new ValidationException(mensajeSinSesion));
        if (tipoRequerido != null && sesion.getTipo() != tipoRequerido) {
            throw new ValidationException(mensajeSinSesion);
        }
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
