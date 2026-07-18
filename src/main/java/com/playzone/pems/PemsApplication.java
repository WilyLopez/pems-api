package com.playzone.pems;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class PemsApplication {

	private final Environment environment;

	@Value("${playzone.frontend-url}")
	private String frontendUrl;

	public PemsApplication(Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
		log.info("Perfiles activos: {}", String.join(",", environment.getActiveProfiles()));
		log.info("playzone.frontend-url resuelto en arranque: {}", frontendUrl);
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
		SpringApplication.run(PemsApplication.class, args);
	}

}
