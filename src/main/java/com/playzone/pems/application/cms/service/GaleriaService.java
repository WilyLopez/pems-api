package com.playzone.pems.application.cms.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.cms.port.in.GestionarGaleriaUseCase;
import com.playzone.pems.domain.cms.model.ImagenGaleria;
import com.playzone.pems.domain.cms.model.enums.CategoriaImagen;
import com.playzone.pems.domain.cms.repository.ImagenGaleriaRepository;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class GaleriaService implements GestionarGaleriaUseCase {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ImagenGaleriaRepository galeriaRepository;
    private final StoragePort             storagePort;
    private final RegistrarLogUseCase     auditoria;
    private final String                  bucketPublico;

    public GaleriaService(ImagenGaleriaRepository galeriaRepository,
                          StoragePort storagePort,
                          RegistrarLogUseCase auditoria,
                          @Value("${supabase.storage.bucket-publico}") String bucketPublico) {
        this.galeriaRepository = galeriaRepository;
        this.storagePort = storagePort;
        this.auditoria = auditoria;
        this.bucketPublico = bucketPublico;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImagenGaleria> listar(Long idSede, Boolean destacada, Pageable pageable) {
        if (Boolean.TRUE.equals(destacada)) {
            return galeriaRepository.findBySedeAndDestacada(idSede, true, pageable);
        }
        return galeriaRepository.findBySede(idSede, pageable);
    }

    @Override
    @Transactional
    public ImagenGaleria subir(Long idSede, byte[] contenido, String nombreArchivo,
                               String contentType, String titulo, String descripcion,
                               String altTexto, CategoriaImagen categoria,
                               int orden, UUID idUsuario) {

        String key = "galeria/" + LocalDateTime.now().format(FMT) + "_" + nombreArchivo;
        String url = storagePort.upload(bucketPublico, key, contenido, contentType);

        try {
            ImagenGaleria imagen = ImagenGaleria.builder()
                    .idSede(idSede)
                    .urlImagen(url)
                    .titulo(titulo)
                    .descripcion(descripcion)
                    .altTexto(altTexto)
                    .categoriaImagen(categoria)
                    .tipoMime(contentType)
                    .tamanioBytes((long) contenido.length)
                    .ordenVisualizacion(orden)
                    .activo(true)
                    .destacada(false)
                    .eliminada(false)
                    .idUsuarioSubio(idUsuario)
                    .build();

            ImagenGaleria guardada = galeriaRepository.save(imagen);

            auditoria.ejecutar(new RegistrarLogUseCase.Command(
                    idUsuario,
                    AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_CMS,
                    "ImagenGaleria", guardada.getId(),
                    null, guardada.getTitulo() != null ? guardada.getTitulo() : nombreArchivo,
                    "Imagen de galería subida",
                    null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

            return guardada;
        } catch (RuntimeException e) {
            storagePort.deleteByUrl(url);
            throw e;
        }
    }

    @Override
    @Transactional
    public void eliminar(Long idImagen) {
        ImagenGaleria imagen = galeriaRepository.findById(idImagen)
                .orElseThrow(() -> new ResourceNotFoundException("ImagenGaleria", idImagen));

        storagePort.deleteByUrl(imagen.getUrlImagen());
        galeriaRepository.deleteById(idImagen);
    }

    @Override
    @Transactional
    public void reordenar(Long idImagen, int nuevoOrden) {
        ImagenGaleria imagen = galeriaRepository.findById(idImagen)
                .orElseThrow(() -> new ResourceNotFoundException("ImagenGaleria", idImagen));

        galeriaRepository.save(imagen.toBuilder().ordenVisualizacion(nuevoOrden).build());
    }

    @Override
    @Transactional
    public void destacar(Long idImagen) {
        ImagenGaleria imagen = galeriaRepository.findById(idImagen)
                .orElseThrow(() -> new ResourceNotFoundException("ImagenGaleria", idImagen));
        galeriaRepository.save(imagen.toBuilder().destacada(true).build());
    }

    @Override
    @Transactional
    public void quitarDestacado(Long idImagen) {
        ImagenGaleria imagen = galeriaRepository.findById(idImagen)
                .orElseThrow(() -> new ResourceNotFoundException("ImagenGaleria", idImagen));
        galeriaRepository.save(imagen.toBuilder().destacada(false).build());
    }

    @Override
    @Transactional
    public ImagenGaleria actualizar(Long idImagen, String titulo, String descripcion,
                                    String altTexto, CategoriaImagen categoria, Integer orden) {
        ImagenGaleria imagen = galeriaRepository.findById(idImagen)
                .orElseThrow(() -> new ResourceNotFoundException("ImagenGaleria", idImagen));

        ImagenGaleria.ImagenGaleriaBuilder builder = imagen.toBuilder();
        if (titulo != null)    builder.titulo(titulo);
        if (descripcion != null) builder.descripcion(descripcion);
        if (altTexto != null)  builder.altTexto(altTexto);
        if (categoria != null) builder.categoriaImagen(categoria);
        if (orden != null)     builder.ordenVisualizacion(orden);

        return galeriaRepository.save(builder.build());
    }
}
