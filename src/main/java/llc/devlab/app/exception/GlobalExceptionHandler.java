package llc.devlab.app.exception;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception, request.getRequestURI(), List.of());
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception, request.getRequestURI(), List.of());
    }

    @ExceptionHandler({UnauthorizedException.class})
    ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, exception, request.getRequestURI(), List.of());
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    ResponseEntity<ApiErrorResponse> handleForbidden(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, exception, request.getRequestURI(), List.of());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldValidationError)
                .toList();

        return build(HttpStatus.BAD_REQUEST, new ValidationException("Validation failed."), request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, exception, request.getRequestURI(), List.of());
    }

    private FieldValidationError toFieldValidationError(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            Exception e,
            String path,
            List<FieldValidationError> fieldErrors
    ) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                e.getMessage(),
                path,
                fieldErrors
        ));
    }
}
