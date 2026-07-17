package com.playzone.pems.application.calendario.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.calendario.dto.command.ConfigurarTarifaCommand;
import com.playzone.pems.application.calendario.port.in.ConfigurarTarifaUseCase;
import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TarifaService implements ConfigurarTarifaUseCase {

    private final TarifaRepository   tarifaRepository;
    private final SupabaseAuthFacade  authFacade;
    private final RegistrarLogUseCase auditoria;

    @Override
    @Transactional
    public Tarifa ejecutar(ConfigurarTarifaCommand command) {
        if (command.getVigenciaHasta() != null
                && command.getVigenciaHasta().isBefore(command.getVigenciaDesde())) {
            throw new ValidationException("vigenciaHasta", "La vigencia final no puede ser anterior a la inicial.");
        }

        if (command.getDuracionMinutos() != null && command.getDuracionMinutos() <= 0) {
            throw new ValidationException("duracionMinutos", "La duracion debe ser mayor a 0 minutos.");
        }

        tarifaRepository.desactivarAnterioresBySedeAndTipoDia(
                command.getIdSede(), command.getTipoDia());

        Tarifa tarifa = Tarifa.builder()
                .idSede(command.getIdSede())
                .tipoDia(command.getTipoDia())
                .precio(command.getPrecio())
                .duracionMinutos(command.getDuracionMinutos())
                .vigenciaDesde(command.getVigenciaDesde())
                .vigenciaHasta(command.getVigenciaHasta())
                .activo(true)
                .build();

        Tarifa resultado = tarifaRepository.save(tarifa);

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_CALENDARIO,
                "Tarifa", resultado.getId(),
                null, command.getPrecio().toPlainString(),
                "Tarifa configurada: " + command.getTipoDia() + " → S/ " + command.getPrecio(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        return resultado;
    }
}