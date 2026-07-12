package com.playzone.pems.domain.contrato.repository;

import com.playzone.pems.domain.contrato.model.Contrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface ContratoRepository {

    Optional<Contrato> findById(Long id);

    Optional<Contrato> findByEventoPrivado(Long idEventoPrivado);

    Contrato save(Contrato contrato);

    Page<Contrato> buscarConFiltros(Long idSede, LocalDate fechaEvento, Pageable pageable);
}
