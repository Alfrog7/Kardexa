package com.kardexa.shared.infrastructure.web;

import com.kardexa.shared.domain.ConflictException;
import com.kardexa.shared.domain.DomainException;
import com.kardexa.shared.domain.NotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas Problem Details (RFC 9457) con mensajes en español.
 *
 * <ul>
 *   <li>{@link NotFoundException} → 404 y {@link ConflictException} → 409.
 *   <li>Cualquier otra {@link DomainException} → 422.
 *   <li>Las tres incluyen el campo {@code rule} (RN-XX o RF-XX).
 *   <li>Errores de validación de Bean Validation → 400, con la lista {@code errors}.
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String PROBLEM_BASE_URI = "https://kardexa.dev/problemas/";

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(DomainException ex) {
        return domainProblem(HttpStatus.UNPROCESSABLE_CONTENT, "regla-de-negocio", ex);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return domainProblem(HttpStatus.NOT_FOUND, "recurso-no-encontrado", ex);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return domainProblem(HttpStatus.CONFLICT, "conflicto", ex);
    }

    private static ProblemDetail domainProblem(HttpStatus status, String type, DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(ex.title());
        problem.setType(URI.create(PROBLEM_BASE_URI + type));
        problem.setProperty("rule", ex.rule());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los campos marcados.");
        problem.setTitle("Datos inválidos");
        problem.setType(URI.create(PROBLEM_BASE_URI + "datos-invalidos"));
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }
}
