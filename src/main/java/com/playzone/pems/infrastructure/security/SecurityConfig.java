package com.playzone.pems.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SupabaseJwtFilter supabaseJwtFilter;

    @Value("${CORS_ORIGINS}")
    private String corsOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/sedes/**",
                                "/api/v1/calendario/**",
                                "/api/v1/feriados",
                                "/api/v1/tarifas",
                                "/api/v1/tarifas/sedes/*/precios",
                                "/api/v1/banners/**",
                                "/api/v1/galeria/**",
                                "/api/v1/cms/**",
                                "/api/v1/contenido/**",
                                "/api/v1/configuracion/publica",
                                "/api/v1/resenas/**",
                                "/api/v1/promociones/**",
                                "/api/v1/actividades/**",
                                "/api/v1/novedades/**",
                                "/api/v1/paquetes/**",
                                "/api/v1/tipos-evento",
                                "/api/v1/zonas/**",
                                "/api/v1/servicios-cotizacion/**",
                                "/api/v1/categorias-servicio",
                                "/api/v1/health/ping"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, 
                                "/api/v1/resenas",
                                "/api/v1/clientes/registro",
                                "/api/v1/auth/**",
                                "/api/v1/contacto"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.setCharacterEncoding("UTF-8");
                            res.getWriter().write(jsonError("unauthorized", e.getMessage()));
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.setCharacterEncoding("UTF-8");
                            res.getWriter().write(jsonError("forbidden", e.getMessage()));
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        if (corsOrigins != null && !corsOrigins.isBlank()) {
            List<String> origins = Arrays.stream(corsOrigins.split(","))
                    .map(String::trim)
                    .toList();
            configuration.setAllowedOrigins(origins);
        } else {
            configuration.setAllowedOrigins(List.of("*"));
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static String jsonError(String error, String message) {
        String safeMsg = message == null ? "" : message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "{\"error\":\"" + error + "\",\"message\":\"" + safeMsg + "\"}";
    }
}