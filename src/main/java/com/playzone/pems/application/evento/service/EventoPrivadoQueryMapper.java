package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.query.EventoCuotaQuery;
import com.playzone.pems.application.evento.dto.query.EventoExtraQuery;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.dto.query.EventoServicioQuery;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.ModalidadPago;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventoPrivadoQueryMapper {

    private final EventoPrivadoRepository  eventoRepository;
    private final ClientePerfilRepository  clientePerfilRepository;
    private final TurnoRepository          turnoRepository;
    private final EventoExtraRepository    eventoExtraRepository;
    private final EventoServicioRepository eventoServicioRepository;
    private final EventoCuotaRepository    cuotaRepository;
    private final ExtraPaqueteRepository   extraPaqueteRepository;
    private final PerfilUsuarioRepository  perfilUsuarioRepository;
    private final VentaRepository          ventaRepository;
    private final VentaPagoRepository      ventaPagoRepository;

    public EventoPrivado obtenerEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", id));
    }

    public ClientePerfil obtenerCliente(Long idCliente) {
        return clientePerfilRepository.buscarPorId(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", idCliente));
    }

    public Turno obtenerTurno(Long idTurno) {
        return turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", idTurno));
    }

    public EventoPrivadoQuery toQuery(EventoPrivado e, ClientePerfil c, Turno t, boolean cargarDetalle) {
        List<EventoExtraQuery> extras = null;
        List<EventoServicioQuery> servicios = null;
        List<EventoCuotaQuery> cuotas = null;

        if (cargarDetalle) {
            extras = eventoExtraRepository.findByEvento(e.getId()).stream()
                    .map(ex -> {
                        String nombre = null;
                        if (ex.getIdExtra() != null) {
                            nombre = extraPaqueteRepository.findById(ex.getIdExtra())
                                    .map(ep -> ep.getNombre()).orElse(null);
                        }
                        return EventoExtraQuery.builder()
                                .id(ex.getId())
                                .idExtra(ex.getIdExtra())
                                .nombreExtra(nombre)
                                .nombreLibre(ex.getNombreLibre())
                                .cantidad(ex.getCantidad())
                                .notas(ex.getNotas())
                                .build();
                    }).toList();

            servicios = eventoServicioRepository.findByEvento(e.getId()).stream()
                    .map(es -> EventoServicioQuery.builder()
                            .id(es.getId())
                            .idServicioCotizacion(es.getIdServicioCotizacion())
                            .idServicioVariante(es.getIdServicioVariante())
                            .nombre(es.getNombreLibre())
                            .precioAcordado(es.getPrecioAcordado())
                            .build())
                    .toList();

            if (e.getModalidadPago() == ModalidadPago.CUOTAS) {
                cuotas = cuotaRepository.findByEventoId(e.getId()).stream()
                        .map(c2 -> EventoCuotaQuery.builder()
                                .id(c2.getId())
                                .numeroCuota(c2.getNumeroCuota())
                                .monto(c2.getMonto())
                                .fechaVencimiento(c2.getFechaVencimiento())
                                .estado(c2.getEstado().getCodigo())
                                .ventaId(c2.getVentaId())
                                .createdAt(c2.getCreatedAt())
                                .build())
                        .toList();
            }
        }

        return EventoPrivadoQuery.builder()
                .id(e.getId())
                .idCliente(e.getIdCliente())
                .nombreCliente(c.nombreCompleto())
                .correoCliente(c.getCorreo())
                .telefonoCliente(c.getTelefono())
                .idSede(e.getIdSede())
                .estado(e.getEstado().getCodigo())
                .idTurno(e.getIdTurno())
                .turno(t.getDescripcion())
                .horaInicio(t.getHoraInicio().toString())
                .horaFin(t.getHoraFin().toString())
                .fechaEvento(e.getFechaEvento())
                .tipoEvento(e.getNombreTipoEvento() != null ? e.getNombreTipoEvento() : e.getTipoEvento())
                .contactoAdicional(e.getContactoAdicional())
                .origenContacto(e.getOrigenContacto())
                .motivoCancelacion(e.getMotivoCancelacion())
                .aforoDeclarado(e.getAforoDeclarado())
                .precioTotalContrato(e.getPrecioContrato())
                .montoAdelanto(e.getMontoAdelanto())
                .montoSaldo(e.calcularMontoSaldo())
                .observaciones(e.getNotasInternas())
                .nombreNino(e.getNombreNino())
                .edadCumple(e.getEdadCumple())
                .idPaquete(e.getPaqueteId())
                .descripcionPersonalizada(e.getDescripcionPersonalizada())
                .presupuestoEstimado(e.getPresupuestoEstimado())
                .esCotizacionPersonalizada(e.isEsCotizacionPersonalizada())
                .usuarioGestor(e.getIdUsuarioGestor() != null
                        ? perfilUsuarioRepository.buscarPorId(e.getIdUsuarioGestor())
                                .map(PerfilUsuario::getNombreCompleto)
                                .orElse(null)
                        : null)
                .estadoOperativo(e.getEstadoOperativo())
                .checklistCompleto(e.isChecklistCompleto())
                .horaInicioReal(e.getHoraInicioReal())
                .horaFinReal(e.getHoraFinReal())
                .extras(extras)
                .servicios(servicios)
                .medioPago(fetchMedioPagoEvento(e.getId()))
                .fechaCreacion(e.getCreatedAt())
                .modalidadPago(e.getModalidadPago().getCodigo())
                .fechaLimitePago(e.getFechaLimitePago())
                .cuotas(cuotas)
                .build();
    }

    private String fetchMedioPagoEvento(Long idEvento) {
        List<Venta> ventas = ventaRepository.findByEventoId(idEvento);
        if (ventas.isEmpty()) return null;
        List<VentaPago> pagos = ventas.stream()
                .flatMap(v -> ventaPagoRepository.findByVentaId(v.getId()).stream())
                .toList();
        if (pagos.isEmpty()) return null;
        long distinct = pagos.stream().map(VentaPago::getMedioPagoCodigo).distinct().count();
        return distinct == 1 ? pagos.get(0).getMedioPagoCodigo() : "MULTIPLE";
    }
}
