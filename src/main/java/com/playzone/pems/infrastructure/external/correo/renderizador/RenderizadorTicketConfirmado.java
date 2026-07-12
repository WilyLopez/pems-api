package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import com.playzone.pems.infrastructure.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorTicketConfirmado implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final SedeRepository sedeRepository;
    private final TicketIngresoPdfService ticketIngresoPdfService;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "PAGO_CONFIRMADO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());
        String nombreSede = sedeRepository.findById(reserva.getIdSede())
                .map(sede -> sede.getNombre())
                .orElse("Sede Principal");

        byte[] pdf = ticketIngresoPdfService.generarTicketPdf(reserva, nombreSede);

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "numeroTicket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "fecha", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "",
                "total", reserva.getTotalPagado() != null ? reserva.getTotalPagado().toPlainString() : "0.00");

        String cuerpoHtml = templateService.procesarTemplate("email-ticket", variables);

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Tu ticket Kiki y Lala — " + reserva.getNumeroTicket())
                .cuerpoHtml(cuerpoHtml)
                .adjuntos(List.of(AdjuntoCorreo.builder()
                        .nombreArchivo("Ticket-" + reserva.getNumeroTicket() + ".pdf")
                        .contenido(pdf)
                        .tipoContenido("application/pdf")
                        .build()))
                .build();
    }
}
