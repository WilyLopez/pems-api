package com.playzone.pems.application.evento.service;

import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.domain.evento.model.EventoExtra;
import com.playzone.pems.domain.evento.model.EventoServicio;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventoServicioExtraWriter {

    private final ServicioCotizacionRepository servicioCotizacionRepository;
    private final ServicioVarianteRepository   servicioVarianteRepository;
    private final EventoServicioRepository     eventoServicioRepository;
    private final EventoExtraRepository        eventoExtraRepository;

    public void persistirServiciosCotizacion(Long idEvento, List<Long> idsServicios, Map<Long, Long> variantesSeleccionadas) {
        if (idsServicios == null || idsServicios.isEmpty()) return;
        Map<Long, Long> variantes = variantesSeleccionadas != null ? variantesSeleccionadas : Map.of();
        List<ServicioCotizacion> seleccionados = servicioCotizacionRepository.findAllActivos().stream()
                .filter(s -> idsServicios.contains(s.getId()))
                .toList();
        List<EventoServicio> servicios = seleccionados.stream()
                .map(s -> construirEventoServicio(idEvento, s, variantes.get(s.getId())))
                .toList();
        if (!servicios.isEmpty()) eventoServicioRepository.saveAll(servicios);
    }

    private EventoServicio construirEventoServicio(Long idEvento, ServicioCotizacion servicio, Long idVarianteSeleccionada) {
        List<ServicioVariante> variantesActivas = servicioVarianteRepository.findByServicio(servicio.getId()).stream()
                .filter(ServicioVariante::isActivo)
                .toList();

        if (variantesActivas.isEmpty()) {
            return EventoServicio.builder()
                    .idEventoPrivado(idEvento)
                    .idServicioCotizacion(servicio.getId())
                    .nombreLibre(servicio.getNombre())
                    .precioAcordado(servicio.getPrecioReferencial())
                    .incluido(true)
                    .build();
        }

        ServicioVariante variante = variantesActivas.stream()
                .filter(v -> v.getId().equals(idVarianteSeleccionada))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Debes seleccionar una variante para el servicio '" + servicio.getNombre() + "'."));

        return EventoServicio.builder()
                .idEventoPrivado(idEvento)
                .idServicioCotizacion(servicio.getId())
                .idServicioVariante(variante.getId())
                .nombreLibre(servicio.getNombre() + " - " + variante.getNombre())
                .precioAcordado(variante.getPrecio())
                .incluido(true)
                .build();
    }

    public void persistirExtras(Long idEvento, List<Long> idsExtras, List<String> extrasLibres) {
        List<EventoExtra> extras = new ArrayList<>();
        if (idsExtras != null) {
            idsExtras.forEach(idExtra -> extras.add(
                    EventoExtra.builder().idEventoPrivado(idEvento).idExtra(idExtra).build()));
        }
        if (extrasLibres != null) {
            extrasLibres.stream().filter(t -> t != null && !t.isBlank()).forEach(texto -> extras.add(
                    EventoExtra.builder().idEventoPrivado(idEvento).nombreLibre(texto).build()));
        }
        if (!extras.isEmpty()) eventoExtraRepository.saveAll(extras);
    }
}
