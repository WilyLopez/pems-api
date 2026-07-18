package com.playzone.pems.shared.exception;

import com.playzone.pems.domain.calendario.exception.ConflictoActividadException;
import com.playzone.pems.shared.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("BusinessException [{}]: {}", ex.getCodigoError(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .codigoError(ex.getCodigoError())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequestsException(
            TooManyRequestsException ex, HttpServletRequest request) {

        log.warn("TooManyRequestsException en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .codigoError("TOO_MANY_REQUESTS")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.CampoError> erroresCampo = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.CampoError.builder()
                        .campo(fe.getField())
                        .mensaje(fe.getDefaultMessage())
                        .valorRechazado(fe.getRejectedValue())
                        .build())
                .toList();

        List<ErrorResponse.CampoError> erroresGlobal = ex.getBindingResult()
                .getGlobalErrors()
                .stream()
                .map(ge -> ErrorResponse.CampoError.builder()
                        .campo(ge.getObjectName())
                        .mensaje(ge.getDefaultMessage())
                        .build())
                .toList();

        List<ErrorResponse.CampoError> todos = new java.util.ArrayList<>();
        todos.addAll(erroresCampo);
        todos.addAll(erroresGlobal);

        log.warn("Error de validación de argumentos en {}: {}", request.getRequestURI(), todos);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .codigoError("VALIDATION_ERROR")
                .message("Error de validación en los datos enviados.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .erroresCampo(todos)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        List<ErrorResponse.CampoError> errores;
        if (ex.getCampo() != null) {
            String mensajeLimpio = ex.getMessage().replace("Campo '" + ex.getCampo() + "': ", "");
            errores = List.of(ErrorResponse.CampoError.builder()
                    .campo(ex.getCampo())
                    .mensaje(mensajeLimpio)
                    .build());
        } else {
            errores = ex.getErrores().stream()
                    .map(msg -> ErrorResponse.CampoError.builder().mensaje(msg).build())
                    .toList();
        }

        log.warn("Error de validación de negocio en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .codigoError(ex.getCodigoError())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .erroresCampo(errores)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConflictoActividadException.class)
    public ResponseEntity<ErrorResponse> handleConflictoActividad(
            ConflictoActividadException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .codigoError("CONFLICTO_ACTIVIDAD")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "forbidden", "message", "No tienes permisos para esta operacion"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "unauthorized", "message", "Autenticacion requerida"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "unauthorized", "message", "Credenciales incorrectas."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .codigoError("method_not_allowed")
                .message("Metodo HTTP '" + ex.getMethod() + "' no soportado en este endpoint.")
                .path(request.getRequestURI())
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String mensaje = String.format("El parámetro '%s' tiene un formato inválido.", ex.getName());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .codigoError("TIPO_PARAMETRO_INVALIDO")
                .message(mensaje)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String rawMessage = ex.getMostSpecificCause().getMessage();
        String mensaje = extraerMensajeTrigger(rawMessage);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .codigoError("db_constraint_violation")
                .message(mensaje)
                .path(request.getRequestURI())
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    private String extraerMensajeTrigger(String rawMessage) {
        if (rawMessage == null) return "Error de integridad en la operacion.";
        int idx = rawMessage.indexOf("ERROR:");
        if (idx >= 0) {
            String rest = rawMessage.substring(idx + 6).trim();
            int nl = rest.indexOf('\n');
            return nl > 0 ? rest.substring(0, nl).trim() : rest;
        }
        return rawMessage;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Cuerpo de solicitud ilegible en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .codigoError("CUERPO_INVALIDO")
                .message("El cuerpo de la solicitud no tiene un formato válido.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException ex, HttpServletRequest request) {
        log.debug("Cliente desconectado en {}", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .codigoError("INTERNAL_ERROR")
                .message("Ocurrió un error interno. Por favor intenta nuevamente.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.internalServerError().body(error);
    }
}