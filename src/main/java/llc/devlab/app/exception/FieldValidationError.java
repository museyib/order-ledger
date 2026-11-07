package llc.devlab.app.exception;

public record FieldValidationError(
        String field,
        String message
) {
}
