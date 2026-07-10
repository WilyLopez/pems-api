package com.playzone.pems.infrastructure.persistence.finanzas.adapter;

import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.MovimientoCajaRepository;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.SesionCajaEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.MovimientoCajaEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.jpa.SesionCajaJpaRepository;
import com.playzone.pems.infrastructure.persistence.finanzas.jpa.MovimientoCajaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MovimientoCajaPersistenceAdapter implements MovimientoCajaRepository {

    private final MovimientoCajaJpaRepository jpaRepository;
    private final SesionCajaJpaRepository     sesionCajaJpaRepository;

    @Override
    public List<MovimientoCaja> findBySesion(Long idSesionCaja) {
        return jpaRepository.findBySesionCaja_IdOrderByFechaCreacionAsc(idSesionCaja)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<MovimientoCaja> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsContraasientoPara(Long idMovimientoOriginal) {
        return jpaRepository.existsByMovimientoAnuladoId(idMovimientoOriginal);
    }

    @Override
    @Transactional
    public MovimientoCaja save(MovimientoCaja movimiento) {
        SesionCajaEntity sesionCaja = sesionCajaJpaRepository
                .getReferenceById(movimiento.getIdSesionCaja());
        MovimientoCajaEntity entity = MovimientoCajaEntity.builder()
                .id(movimiento.getId())
                .sesionCaja(sesionCaja)
                .tipo(movimiento.getTipo())
                .concepto(movimiento.getConcepto())
                .monto(movimiento.getMonto())
                .medioPago(movimiento.getMedioPago())
                .categoriaRetiro(movimiento.getCategoriaRetiro())
                .idRegistroIngreso(movimiento.getIdRegistroIngreso())
                .idRegistroEgreso(movimiento.getIdRegistroEgreso())
                .ventaId(movimiento.getIdVenta())
                .esManual(movimiento.isEsManual())
                .naturaleza(movimiento.getNaturaleza())
                .movimientoAnuladoId(movimiento.getIdMovimientoAnulado())
                .createdBy(movimiento.getIdUsuarioRegistra())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private MovimientoCaja toDomain(MovimientoCajaEntity e) {
        return MovimientoCaja.builder()
                .id(e.getId())
                .idSesionCaja(e.getSesionCaja().getId())
                .tipo(e.getTipo())
                .concepto(e.getConcepto())
                .monto(e.getMonto())
                .medioPago(e.getMedioPago())
                .categoriaRetiro(e.getCategoriaRetiro())
                .idRegistroIngreso(e.getIdRegistroIngreso())
                .idRegistroEgreso(e.getIdRegistroEgreso())
                .idVenta(e.getVentaId())
                .esManual(e.isEsManual())
                .naturaleza(e.getNaturaleza())
                .idMovimientoAnulado(e.getMovimientoAnuladoId())
                .idUsuarioRegistra(e.getCreatedBy())
                .fechaCreacion(e.getFechaCreacion())
                .build();
    }
}
