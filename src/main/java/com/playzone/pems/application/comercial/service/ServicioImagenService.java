package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.SubirServicioImagenCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioImagenQuery;
import com.playzone.pems.application.comercial.port.in.GestionarServicioImagenesUseCase;
import com.playzone.pems.domain.comercial.model.ServicioImagen;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioImagenRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.domain.storage.StorageCarpeta;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.BusinessException;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class ServicioImagenService implements GestionarServicioImagenesUseCase {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final int MAX_IMAGENES_POR_GRUPO = 8;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    private final ServicioImagenRepository     repository;
    private final ServicioCotizacionRepository servicioRepository;
    private final ServicioVarianteRepository   varianteRepository;
    private final StoragePort                  storagePort;
    private final SupabaseAuthFacade           authFacade;
    private final RegistrarLogUseCase          auditoria;
    private final String                       bucketPublico;

    public ServicioImagenService(ServicioImagenRepository repository,
                                  ServicioCotizacionRepository servicioRepository,
                                  ServicioVarianteRepository varianteRepository,
                                  StoragePort storagePort,
                                  SupabaseAuthFacade authFacade,
                                  RegistrarLogUseCase auditoria,
                                  @Value("${supabase.storage.bucket-publico}") String bucketPublico) {
        this.repository = repository;
        this.servicioRepository = servicioRepository;
        this.varianteRepository = varianteRepository;
        this.storagePort = storagePort;
        this.authFacade = authFacade;
        this.auditoria = auditoria;
        this.bucketPublico = bucketPublico;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioImagenQuery> listarPorServicio(Long idServicio) {
        return repository.findByServicio(idServicio).stream().map(this::toQuery).toList();
    }

    @Override
    public ServicioImagenQuery subir(Long idServicio, SubirServicioImagenCommand command) {
        servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("ServicioCotizacion", idServicio));
        if (command.getIdVariante() != null) {
            validarVarianteDelServicio(idServicio, command.getIdVariante());
        }
        validarArchivo(command.getContentType(), command.getContenido().length);
        validarCantidad(idServicio, command.getIdVariante());

        String key = StorageCarpeta.SERVICIOS.valor() + "/" + LocalDateTime.now().format(FMT) + "_" + sanitizarNombre(command.getNombreArchivo());
        String url = storagePort.upload(bucketPublico, key, command.getContenido(), command.getContentType());

        try {
            ServicioImagen imagen = ServicioImagen.builder()
                    .idServicio(idServicio)
                    .idVariante(command.getIdVariante())
                    .archivoPath(url)
                    .altTexto(command.getAltTexto())
                    .orden(command.getOrden())
                    .esPrincipal(false)
                    .build();
            ServicioImagenQuery resultado = toQuery(repository.save(imagen));
            auditoria.ejecutar(new RegistrarLogUseCase.Command(
                    authFacade.usuarioActualId().orElse(null),
                    AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_COMERCIAL,
                    "ServicioImagen", resultado.getId(),
                    null, url,
                    "Imagen de servicio subida",
                    null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
            return resultado;
        } catch (RuntimeException e) {
            storagePort.deleteByUrl(url);
            throw e;
        }
    }

    @Override
    public void eliminar(Long idServicio, Long id) {
        ServicioImagen imagen = obtenerDeServicio(idServicio, id);
        storagePort.deleteByUrl(imagen.getArchivoPath());
        repository.deleteById(id);
    }

    @Override
    public ServicioImagenQuery reordenar(Long idServicio, Long id, int nuevoOrden) {
        ServicioImagen imagen = obtenerDeServicio(idServicio, id);
        List<ServicioImagen> delGrupo = repository.findByServicio(idServicio).stream()
                .filter(i -> Objects.equals(i.getIdVariante(), imagen.getIdVariante()))
                .toList();
        delGrupo.stream()
                .filter(i -> i.getOrden() == nuevoOrden && !i.getId().equals(id))
                .findFirst()
                .ifPresent(otra -> repository.save(otra.toBuilder().orden(imagen.getOrden()).build()));
        return toQuery(repository.save(imagen.toBuilder().orden(nuevoOrden).build()));
    }

    @Override
    public ServicioImagenQuery marcarPrincipal(Long idServicio, Long id) {
        ServicioImagen imagen = obtenerDeServicio(idServicio, id);
        repository.limpiarPrincipal(idServicio, imagen.getIdVariante());
        return toQuery(repository.save(imagen.toBuilder().esPrincipal(true).build()));
    }

    private void validarVarianteDelServicio(Long idServicio, Long idVariante) {
        ServicioVariante variante = varianteRepository.findById(idVariante)
                .orElseThrow(() -> new ResourceNotFoundException("ServicioVariante", idVariante));
        if (!variante.getIdServicio().equals(idServicio)) {
            throw new ResourceNotFoundException("ServicioVariante", idVariante);
        }
    }

    private void validarArchivo(String contentType, long tamanioBytes) {
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new BusinessException("Tipo de archivo no permitido: " + contentType, HttpStatus.BAD_REQUEST);
        }
        if (tamanioBytes > MAX_BYTES) {
            throw new BusinessException("El archivo supera el limite de 8 MB.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarCantidad(Long idServicio, Long idVariante) {
        long actuales = idVariante != null
                ? repository.countByVariante(idVariante)
                : repository.countByServicioSinVariante(idServicio);
        if (actuales >= MAX_IMAGENES_POR_GRUPO) {
            throw new BusinessException(
                    "Se alcanzo el limite de " + MAX_IMAGENES_POR_GRUPO + " imagenes.", HttpStatus.BAD_REQUEST);
        }
    }

    private ServicioImagen obtenerDeServicio(Long idServicio, Long id) {
        ServicioImagen imagen = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServicioImagen", id));
        if (!imagen.getIdServicio().equals(idServicio)) {
            throw new ResourceNotFoundException("ServicioImagen", id);
        }
        return imagen;
    }

    private String sanitizarNombre(String nombre) {
        if (nombre == null) return "archivo";
        return nombre.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    private ServicioImagenQuery toQuery(ServicioImagen i) {
        return ServicioImagenQuery.builder()
                .id(i.getId())
                .idServicio(i.getIdServicio())
                .idVariante(i.getIdVariante())
                .url(i.getArchivoPath())
                .altTexto(i.getAltTexto())
                .orden(i.getOrden())
                .esPrincipal(i.isEsPrincipal())
                .build();
    }
}
