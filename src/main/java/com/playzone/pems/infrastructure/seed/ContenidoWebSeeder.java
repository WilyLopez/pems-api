package com.playzone.pems.infrastructure.seed;

import com.playzone.pems.domain.cms.model.ContenidoWeb;
import com.playzone.pems.domain.cms.repository.ContenidoWebRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class ContenidoWebSeeder implements ApplicationRunner {

    private final ContenidoWebRepository contenidoRepository;

    @Override
    public void run(ApplicationArguments args) {
        int orden = 0;

        sembrar("HOME", "home.hero.badge",
                "El local más divertido de Chiclayo", orden++);
        sembrar("HOME", "home.hero.titulo_linea1",
                "Diversión", orden++);
        sembrar("HOME", "home.hero.titulo_linea2",
                "sin límites", orden++);
        sembrar("HOME", "home.hero.parrafo",
                "El espacio donde los niños son los protagonistas. Juegos, celebraciones privadas y momentos que jamás olvidarán, en un ambiente seguro y lleno de alegría.",
                orden++);
        sembrar("HOME", "home.seguridad.titulo",
                "La seguridad de tus hijos, nuestra prioridad", orden++);
        sembrar("HOME", "home.seguridad.subtitulo",
                "Cada rincón de Kiki y Lala está diseñado pensando en el bienestar de los niños",
                orden++);
        sembrar("HOME", "home.hero.stats.familias",
                "+500", orden++);
        sembrar("HOME", "home.hero.stats.calificacion",
                "4.9", orden++);
        sembrar("HOME", "home.hero.stats.anio_fundacion",
                "2023", orden++);

        sembrar("NOSOTROS", "nosotros.historia.badge",
                "Nuestra historia", orden++);
        sembrar("NOSOTROS", "nosotros.historia.titulo",
                "Nacimos de un sueño familiar", orden++);
        sembrar("NOSOTROS", "nosotros.historia.parrafo1",
                "Kiki y Lala nació con una misión clara: crear un espacio donde los niños pudieran ser completamente libres de jugar, reír y descubrir el mundo de una manera segura y divertida.",
                orden++);
        sembrar("NOSOTROS", "nosotros.historia.parrafo2",
                "Lo que comenzó como un pequeño local con pocas atracciones, hoy es uno de los centros de entretenimiento infantil más queridos de Chiclayo, con más de 15 zonas de juego y cientos de eventos realizados.",
                orden++);
        sembrar("NOSOTROS", "nosotros.historia.parrafo3",
                "Nuestros personajes Kiki y Lala representan la amistad, la diversión y la magia de la infancia.",
                orden++);
        sembrar("NOSOTROS", "nosotros.valores.titulo",
                "Nuestros valores", orden++);
        sembrar("NOSOTROS", "nosotros.valores.items",
                "[{\"titulo\":\"Seguridad primero\",\"descripcion\":\"Todas nuestras instalaciones cumplen estrictos estándares de seguridad. Revisión diaria de equipos y supervisión constante del personal.\"},{\"titulo\":\"Amor por los niños\",\"descripcion\":\"Cada detalle del local está pensado para que los niños se sientan felices, libres y especiales. Su sonrisa es nuestra mayor recompensa.\"},{\"titulo\":\"Excelencia en servicio\",\"descripcion\":\"Nos comprometemos a superar las expectativas en cada visita. Personal capacitado, instalaciones limpias y atención personalizada.\"}]",
                orden++);

        sembrar("ZONA_JUEGOS", "zona.reglamento.titulo",
                "Reglamento del local", orden++);
        sembrar("ZONA_JUEGOS", "zona.reglamento.subtitulo",
                "Para garantizar la seguridad y diversión de todos los niños", orden++);
        sembrar("ZONA_JUEGOS", "zona.reglamento.items",
                "[\"Niños de 1 a 12 años\",\"Calcetines obligatorios para todos\",\"Prohibido ingresar con comida externa\",\"Adultos deben permanecer en el local\",\"Sin objetos punzantes o peligrosos\",\"Respetar el aforo por zona\"]",
                orden++);
    }

    private void sembrar(String seccion, String clave, String valor, int orden) {
        try {
            if (contenidoRepository.existsBySeccionAndClave(seccion, clave)) return;
            contenidoRepository.save(ContenidoWeb.builder()
                    .seccionCodigo(seccion)
                    .tipoContenidoCodigo("TEXTO")
                    .clave(clave)
                    .valorEs(valor)
                    .metadatos("{}")
                    .visible(true)
                    .version(1)
                    .ordenVisualizacion(orden)
                    .build());
            log.info("Contenido web sembrado: {}/{}", seccion, clave);
        } catch (Exception e) {
            log.warn("No se pudo sembrar contenido {}/{}: {}", seccion, clave, e.getMessage());
        }
    }
}
