package com.playzone.pems.application.cms.service;

import com.playzone.pems.application.cms.dto.command.EditarContenidoCommand;
import com.playzone.pems.application.cms.dto.query.ContenidoWebQuery;
import com.playzone.pems.application.cms.port.in.ConsultarContenidoWebUseCase;
import com.playzone.pems.application.cms.port.in.EditarContenidoWebUseCase;
import com.playzone.pems.domain.cms.exception.ContenidoNotFoundException;
import com.playzone.pems.domain.cms.model.ContenidoWeb;
import com.playzone.pems.domain.cms.repository.ContenidoWebRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContenidoWebService implements EditarContenidoWebUseCase, ConsultarContenidoWebUseCase {

    private final ContenidoWebRepository contenidoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ContenidoWebQuery> listar(String seccionCodigo, String clave, Pageable pageable) {
        return contenidoRepository.findAll(seccionCodigo, clave, pageable).map(this::toQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContenidoWebQuery> listarPublico() {
        return contenidoRepository.findVisibles().stream().map(this::toQuery).toList();
    }

    @Override
    @Transactional
    public ContenidoWebQuery ejecutar(EditarContenidoCommand command) {
        ContenidoWeb contenido = contenidoRepository.findById(command.getIdContenido())
                .orElseThrow(() -> new ContenidoNotFoundException(command.getIdContenido()));

        ContenidoWeb actualizado = contenido.toBuilder()
                .valorEs(command.getValorEs() != null
                        ? command.getValorEs() : contenido.getValorEs())
                .valorEn(command.getValorEn() != null
                        ? command.getValorEn() : contenido.getValorEn())
                .imagenUrl(command.getImagenUrl() != null
                        ? command.getImagenUrl() : contenido.getImagenUrl())
                .descripcion(command.getDescripcion() != null
                        ? command.getDescripcion() : contenido.getDescripcion())
                .metadatos(command.getMetadatos() != null
                        ? command.getMetadatos() : contenido.getMetadatos())
                .visible(command.getVisible() != null
                        ? command.getVisible() : contenido.isVisible())
                .ordenVisualizacion(command.getOrdenVisualizacion() != null
                        ? command.getOrdenVisualizacion() : contenido.getOrdenVisualizacion())
                .idUsuarioEditor(command.getIdUsuarioEditor())
                .build();

        return toQuery(contenidoRepository.save(actualizado));
    }

    private ContenidoWebQuery toQuery(ContenidoWeb c) {
        return ContenidoWebQuery.builder()
                .id(c.getId())
                .seccion(c.getSeccionCodigo())
                .tipoContenido(c.getTipoContenidoCodigo())
                .clave(c.getClave())
                .valorEs(c.getValorEs())
                .valorEn(c.getValorEn())
                .imagenUrl(c.getImagenUrl())
                .descripcion(c.getDescripcion())
                .ordenVisualizacion(c.getOrdenVisualizacion())
                .visible(c.isVisible())
                .version(c.getVersion())
                .metadatos(c.getMetadatos())
                .activo(c.isActivo())
                .fechaActualizacion(c.getFechaActualizacion())
                .build();
    }
}