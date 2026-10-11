package com.minishiftsync.web;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.minishiftsync.service.ApiException;

/**
 * Turns exceptions into consistent JSON error responses (RFC 9457 "problem details"), so the
 * frontend can always show {@code detail} to the user.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		return problem(ex.getStatus(), ex.getMessage());
	}

	/** A field failed a Bean Validation annotation such as @NotBlank. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new TreeMap<>(); // sorted by field name
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		String detail = fieldErrors.entrySet().stream()
				.map(e -> e.getKey() + " " + e.getValue())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Invalid request.");
		ResponseEntity<ProblemDetail> response = problem(HttpStatus.BAD_REQUEST, detail);
		response.getBody().setProperty("fieldErrors", fieldErrors);
		return response;
	}

	/** Malformed JSON, or a value like "2026-13-45" that can't become a LocalDate. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
		return problem(HttpStatus.BAD_REQUEST,
				"Request body is not valid JSON, or a value has the wrong format (dates are yyyy-MM-dd, times are HH:mm).");
	}

	/** A URL parameter of the wrong type, e.g. /api/employees/abc. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "': " + ex.getValue());
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

}
