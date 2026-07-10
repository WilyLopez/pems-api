package com.playzone.pems.shared.ratelimit;

import com.playzone.pems.shared.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    private final Map<String, List<Instant>> peticiones = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimited)")
    public void verificarLimite(JoinPoint joinPoint, RateLimited rateLimited) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = obtenerIpCliente(request);
        String metodo = joinPoint.getSignature().toShortString();
        String clave = ip + ":" + metodo;

        Instant ahora = Instant.now();
        long limiteSegundos = rateLimited.durationInSeconds();
        int maxPeticiones = rateLimited.requests();

        List<Instant> tiempos = peticiones.computeIfAbsent(clave, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (tiempos) {
            tiempos.removeIf(t -> t.plusSeconds(limiteSegundos).isBefore(ahora));

            if (tiempos.size() >= maxPeticiones) {
                log.warn("Rate limit excedido para IP {} en metodo {}. Peticiones en ventana: {}", ip, metodo, tiempos.size());
                throw new TooManyRequestsException("Has excedido el limite de peticiones. Por favor, intenta de nuevo en unos momentos.");
            }

            tiempos.add(ahora);
        }
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
