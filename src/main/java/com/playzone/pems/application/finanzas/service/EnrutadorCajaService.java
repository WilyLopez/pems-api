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

    private final SesionCajaRepository     sesionCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;

    public void registrarIngresoEfectivo(UUID cobrador, String medioPago, BigDecimal monto,
                                         String concepto, Long ventaId) {
        enrutar(cobrador, medioPago, monto, concepto, ventaId, null, null, MSG_SIN_CAJA);
    }

    public void registrarIngresoEfectivoAdministrativo(UUID gestor, String medioPago, BigDecimal monto,
                                                       String concepto, Long ventaId) {
        enrutar(gestor, medioPago, monto, concepto, ventaId, null,
                TipoSesionCaja.ADMINISTRATIVA, MSG_SIN_CAJA_ADMINISTRATIVA);
    }

    public void registrarIngresoManualEfectivo(UUID gestor, String medioPago, BigDecimal monto,
                                               String concepto, Long registroIngresoId) {
        enrutar(gestor, medioPago, monto, concepto, null, registroIngresoId,
                TipoSesionCaja.ADMINISTRATIVA, MSG_SIN_CAJA_ADMINISTRATIVA_INGRESO);
    }

    private void enrutar(UUID usuario, String medioPago, BigDecimal monto, String concepto,
                         Long ventaId, Long registroIngresoId,
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
                .tipo(TipoMovimientoCaja.INGRESO)
                .concepto(concepto)
                .monto(monto)
                .medioPago(medioPago)
                .idVenta(ventaId)
                .idRegistroIngreso(registroIngresoId)
                .esManual(false)
                .idUsuarioRegistra(usuario)
                .build());
        sesionCajaRepository.incrementarIngresos(sesion.getId(), monto);
    }
}
