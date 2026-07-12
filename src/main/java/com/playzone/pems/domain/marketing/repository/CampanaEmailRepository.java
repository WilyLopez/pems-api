package com.playzone.pems.domain.marketing.repository;

import com.playzone.pems.domain.marketing.model.CampanaEmail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CampanaEmailRepository {

    Optional<CampanaEmail> findById(Long id);

    Page<CampanaEmail> findAll(Pageable pageable);

    List<CampanaEmail> findProgramadasParaEnviar();

    List<CampanaEmail> findByEstado(String estado);

    CampanaEmail save(CampanaEmail campana);

    void actualizarEstado(Long id, String estado);

    void incrementarEnviados(Long id, int cantidad);

    void incrementarFallidos(Long id, int cantidad);
}
