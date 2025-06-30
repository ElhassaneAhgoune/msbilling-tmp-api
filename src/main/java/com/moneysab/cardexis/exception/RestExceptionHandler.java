package com.moneysab.cardexis.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex, @NonNull final HttpHeaders headers, @NonNull final HttpStatusCode status, @NonNull final WebRequest request
    ) {
        final Map<String, List<String>> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>()).add(error.getDefaultMessage());
        }

        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(status, "Request validation failed")
                .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                .withProperty("errors", errors)
                .build();

        return new ResponseEntity<>(problemDetail, status);
    }



    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestCookieException(final MissingRequestCookieException ex) {
        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(BAD_REQUEST, "Required cookie is missing")
                .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                .build();

        return new ResponseEntity<>(problemDetail, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException ex) {
        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(BAD_REQUEST, "Parameter [%s] contains an invalid value".formatted(ex.getName()))
                .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                .build();

        return new ResponseEntity<>(problemDetail, BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(final AuthenticationException ex) {
        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(UNAUTHORIZED, ex.getMessage())
                .withErrorType(ErrorType.UNAUTHORIZED)
                .build();

        if (log.isDebugEnabled()) {
            log.debug("Authorization exception stack trace: ", ex);
        }

        return new ResponseEntity<>(problemDetail, UNAUTHORIZED);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(final ConstraintViolationException ex, @NonNull final WebRequest request) {
        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(CONFLICT, "Error while processing the request")
                .withErrorType(ErrorType.RESOURCE_ALREADY_EXISTS)
                .build();

        return new ResponseEntity<>(problemDetail, CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex);

        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(INTERNAL_SERVER_ERROR, "An unexpected error occurred")
                .withErrorType(ErrorType.UNKNOWN_SERVER_ERROR)
                .build();

        return new ResponseEntity<>(problemDetail, INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        
        log.error("File upload size exceeded: {}", ex.getMessage());
        
        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(PAYLOAD_TOO_LARGE, "Maximum upload size exceeded")
                .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                .build();

        return new ResponseEntity<>(problemDetail, PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler({FileSizeLimitExceededException.class, SizeLimitExceededException.class})
    public ResponseEntity<ProblemDetail> handleFileSizeLimitExceededException(final Exception ex) {
        log.error("File size limit exceeded: {}", ex.getMessage());
        
        final var problemDetail = ProblemDetailBuilder.forStatusAndDetail(PAYLOAD_TOO_LARGE, "Maximum upload size exceeded")
                .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                .build();

        return new ResponseEntity<>(problemDetail, PAYLOAD_TOO_LARGE);
    }

}