package com.playzone.pems.application.calendario.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.calendario.dto.command.ConfigurarTarifaCommand;
import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaServiceTest {

    @Mock private TarifaRepository tarifaRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;

    private TarifaService service;

    private ConfigurarTarifaCommand.ConfigurarTarifaCommandBuilder comandoBase() {
        return ConfigurarTarifaCommand.builder()
                .idSede(1L)
                .tipoDia(TipoDia.FIN_SEMANA_FERIADO)
                .precio(new BigDecimal("25.00"))
                .vigenciaDesde(LocalDate.now());
    }

    @Test
    void testConfigurarTarifaConDuracionLaGuardaEnMinutos() {
        service = new TarifaService(tarifaRepository, authFacade, auditoria);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(tarifaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigurarTarifaCommand comando = comandoBase().duracionMinutos(120).build();

        Tarifa resultado = service.ejecutar(comando);

        ArgumentCaptor<Tarifa> captor = ArgumentCaptor.forClass(Tarifa.class);
        verify(tarifaRepository).save(captor.capture());
        assertEquals(120, captor.getValue().getDuracionMinutos());
        assertEquals(120, resultado.getDuracionMinutos());
    }

    @Test
    void testConfigurarTarifaSinDuracionQuedaTodoElDia() {
        service = new TarifaService(tarifaRepository, authFacade, auditoria);
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());
        when(tarifaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigurarTarifaCommand comando = comandoBase().build();

        Tarifa resultado = service.ejecutar(comando);

        assertNull(resultado.getDuracionMinutos());
    }

    @Test
    void testConfigurarTarifaConDuracionCeroLanzaValidationException() {
        service = new TarifaService(tarifaRepository, authFacade, auditoria);

        ConfigurarTarifaCommand comando = comandoBase().duracionMinutos(0).build();

        assertThrows(ValidationException.class, () -> service.ejecutar(comando));
    }

    @Test
    void testConfigurarTarifaConDuracionNegativaLanzaValidationException() {
        service = new TarifaService(tarifaRepository, authFacade, auditoria);

        ConfigurarTarifaCommand comando = comandoBase().duracionMinutos(-30).build();

        assertThrows(ValidationException.class, () -> service.ejecutar(comando));
    }
}
