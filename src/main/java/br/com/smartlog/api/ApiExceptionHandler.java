package br.com.smartlog.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.servlet.http.HttpServletRequest;

import br.com.smartlog.domain.exception.PersistenceException;
import br.com.smartlog.domain.exception.ResourceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemResponse> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso não encontrado", request);
    }

    @ExceptionHandler(PersistenceException.class)
    ResponseEntity<ProblemResponse> persistenceFailure(PersistenceException exception, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "PERSISTENCE_FAILURE",
                "Não foi possível acessar o armazenamento", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemResponse> accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acesso não permitido", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "valor inválido" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        return ResponseEntity.badRequest().body(new ProblemResponse(Instant.now(), 400,
                "VALIDATION_ERROR", "Dados da requisição inválidos", request.getRequestURI(), fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemResponse> malformed(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Requisição inválida", request);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ResponseEntity<ProblemResponse> serviceStatus(org.springframework.web.server.ResponseStatusException e, HttpServletRequest request) {
        return response(HttpStatus.valueOf(e.getStatusCode().value()), "SERVICE_ERROR", e.getReason(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> unexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno inesperado", request);
    }

    private static ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ProblemResponse(Instant.now(), status.value(), code, message,
                request.getRequestURI(), null));
    }

    public record ProblemResponse(Instant timestamp, int status, String code, String message, String path,
            Map<String, String> fields) {
    }
}
