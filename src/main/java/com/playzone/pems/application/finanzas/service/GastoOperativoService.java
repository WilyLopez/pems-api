package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.finanzas.dto.command.AnularGastoOperativoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarGastoOperativoCommand;
import com.playzone.pems.application.finanzas.dto.query.GastoOperativoQuery;
import com.playzone.pems.application.finanzas.port.in.GestionarGastoOperativoUseCase;
import com.playzone.pems.domain.finanzas.model.GastoOperativoDiario;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.GastoOperativoDiarioRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GastoOperativoService implements GestionarGastoOperativoUseCase {

    private final GastoOperativoDiarioRepository gastoOperativoRepository;
    private final SedeScopeValidator             sedeScope;

    @Override
    public GastoOperativoQuery registrar(RegistrarGastoOperativoCommand command) {
        GastoOperativoDiario gasto = GastoOperativoDiario.builder()
                .idSede(command.getIdSede())
                .fecha(command.getFecha())
                .descripcion(command.getDescripcion())
                .monto(command.getMonto())
                .comprobanteUrl(command.getComprobanteUrl())
                .idUsuarioRegistra(command.getIdUsuarioRegistra())
                .build();
        return toQuery(gastoOperativoRepository.save(gasto));
    }

    @Override
    public GastoOperativoQuery anular(AnularGastoOperativoCommand command) {
        if (command.getMotivo() == null || command.getMotivo().isBlank()) {
            throw new ValidationException("El motivo de anulacion es obligatorio.");
        }
        GastoOperativoDiario original = gastoOperativoRepository.findById(command.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Gasto operativo no encontrado."));
        sedeScope.validarAcceso(original.getIdSede());
        if (original.esContraasiento()) {
            throw new ValidationException("Un contraasiento no puede anularse.");
        }
        if (gastoOperativoRepository.existsAnuladoPara(original.getId())) {
            throw new ValidationException("El gasto operativo ya fue anulado.");
        }

        GastoOperativoDiario contraasiento = GastoOperativoDiario.builder()
                .idSede(original.getIdSede())
                .fecha(original.getFecha())
                .descripcion("Anulacion gasto operativo #" + original.getId() + " | " + command.getMotivo().trim())
                .monto(original.getMonto())
                .naturaleza(NaturalezaMovimientoCaja.CONTRAASIENTO)
                .idGastoAnulado(original.getId())
                .idUsuarioRegistra(command.getIdUsuarioAnula())
                .build();
        try {
            return toQuery(gastoOperativoRepository.save(contraasiento));
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("El gasto operativo ya fue anulado.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GastoOperativoQuery> listarPorFecha(Long idSede, LocalDate fecha) {
        return gastoOperativoRepository.findBySedeAndFecha(idSede, fecha).stream()
                .map(this::toQuery).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GastoOperativoQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return gastoOperativoRepository.findBySedeAndRangoFecha(idSede, inicio, fin).stream()
                .map(this::toQuery).toList();
    }

    private GastoOperativoQuery toQuery(GastoOperativoDiario g) {
        return GastoOperativoQuery.builder()
                .id(g.getId())
                .idSede(g.getIdSede())
                .fecha(g.getFecha())
                .descripcion(g.getDescripcion())
                .monto(g.getMonto())
                .comprobanteUrl(g.getComprobanteUrl())
                .naturaleza(g.getNaturaleza())
                .idGastoAnulado(g.getIdGastoAnulado())
                .fechaCreacion(g.getFechaCreacion())
                .build();
    }
}
