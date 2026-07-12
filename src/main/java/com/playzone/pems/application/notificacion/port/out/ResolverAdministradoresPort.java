package com.playzone.pems.application.notificacion.port.out;

import java.util.List;
import java.util.UUID;

public interface ResolverAdministradoresPort {

    List<UUID> obtenerIdsAdministradoresActivos();
}
