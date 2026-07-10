package com.playzone.pems.infrastructure.persistence.preferencia.mapper;

import com.playzone.pems.domain.preferencia.model.PreferenciaUsuario;
import com.playzone.pems.infrastructure.persistence.preferencia.entity.PreferenciaUsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PreferenciaUsuarioMapper {

    public PreferenciaUsuario toDomain(PreferenciaUsuarioEntity e) {
        Map<String, Object> x = e.getPreferenciasExtras() != null ? e.getPreferenciasExtras() : Map.of();
        return PreferenciaUsuario.builder()
                .usuarioId(e.getUsuarioId())
                .tema(e.getTema())
                .tipografia(str(x, "tipografia", "Inter"))
                .tamanioFuente(str(x, "tamanioFuente", "NORMAL"))
                .sonidoNotificaciones(bool(x, "sonidoNotificaciones", false))
                .notificacionesPush(bool(x, "notificacionesPush", true))
                .notificacionesEmail(bool(x, "notificacionesEmail", true))
                .notificacionesVisuales(bool(x, "notificacionesVisuales", true))
                .badgesDinamicos(bool(x, "badgesDinamicos", true))
                .fechaCreacion(e.getCreatedAt())
                .fechaActualizacion(e.getUpdatedAt())
                .build();
    }

    public PreferenciaUsuarioEntity toEntity(PreferenciaUsuario d) {
        Map<String, Object> extras = buildExtras(d);
        return PreferenciaUsuarioEntity.builder()
                .usuarioId(d.getUsuarioId())
                .tema(d.getTema() != null ? d.getTema() : "SYSTEM")
                .preferenciasExtras(extras)
                .build();
    }

    private Map<String, Object> buildExtras(PreferenciaUsuario d) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipografia",             d.getTipografia() != null ? d.getTipografia() : "Inter");
        m.put("tamanioFuente",          d.getTamanioFuente() != null ? d.getTamanioFuente() : "NORMAL");
        m.put("sonidoNotificaciones",   d.isSonidoNotificaciones());
        m.put("notificacionesPush",     d.isNotificacionesPush());
        m.put("notificacionesEmail",    d.isNotificacionesEmail());
        m.put("notificacionesVisuales", d.isNotificacionesVisuales());
        m.put("badgesDinamicos",        d.isBadgesDinamicos());
        return m;
    }

    private static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean b ? b : def;
    }

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String s ? s : def;
    }
}
