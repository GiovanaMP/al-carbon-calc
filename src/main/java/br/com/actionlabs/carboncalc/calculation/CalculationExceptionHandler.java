package br.com.actionlabs.carboncalc.calculation;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CalculationExceptionHandler {

  @ExceptionHandler(CalculationNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleCalculationNotFound(
      CalculationNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(CalculationInfoNotProvidedException.class)
  public ResponseEntity<Map<String, String>> handleCalculationInfoNotProvided(
      CalculationInfoNotProvidedException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField, fe -> fe.getDefaultMessage(), (a, b) -> a));
    return ResponseEntity.badRequest().body(errors);
  }
}
