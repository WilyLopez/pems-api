package com.playzone.pems.infrastructure.persistence.contrato.mapper;

import com.playzone.pems.domain.contrato.model.Contrato;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.persistence.contrato.entity.ContratoEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContratoEntityMapper {

    private final ClientePerfilRepository clientePerfilRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public Contrato toDomain(ContratoEntity e) {
        if (e == null) return null;
        var ev = e.getEventoPrivado();

        BigDecimal saldo = BigDecimal.ZERO;
        if (ev.getPrecioContrato() != null) {
            saldo = ev.getPrecioContrato().subtract(
                    ev.getMontoAdelanto() != null ? ev.getMontoAdelanto() : BigDecimal.ZERO);
        }

        Optional<ClientePerfil> cliente = clientePerfilRepository.buscarPorId(ev.getClienteId());

        return Contrato.builder()
                .id(e.getId())
                .idEventoPrivado(ev.getId())
                .idCliente(ev.getClienteId())
                .archivoPdfUrl(e.getArchivoPdfUrl())
                .idUsuarioCarga(e.getCargadoPor())
                .usuarioCarga(e.getCargadoPor() != null
                        ? perfilUsuarioRepository.buscarPorId(e.getCargadoPor())
                                .map(u -> u.getNombreCompleto()).orElse(null)
                        : null)
                .fechaCarga(e.getCargadoAt())
                .nombreCliente(cliente.map(ClientePerfil::nombreCompleto).orElse(null))
                .correoCliente(cliente.map(ClientePerfil::getCorreo).orElse(null))
                .tipoEvento(ev.getTipoEvento())
                .fechaEvento(ev.getFechaEvento())
                .turno(ev.getTurno().getNombre())
                .aforoDeclarado(ev.getAforoDeclarado())
                .precioTotalContrato(ev.getPrecioContrato())
                .montoAdelanto(ev.getMontoAdelanto())
                .saldoPendiente(saldo)
                .build();
    }

    public ContratoEntity toEntity(Contrato d, EventoPrivadoEntity evento) {
        if (d == null) return null;
        return ContratoEntity.builder()
                .id(d.getId())
                .eventoPrivado(evento)
                .archivoPdfUrl(d.getArchivoPdfUrl())
                .cargadoPor(d.getIdUsuarioCarga())
                .cargadoAt(d.getFechaCarga())
                .build();
    }
}
