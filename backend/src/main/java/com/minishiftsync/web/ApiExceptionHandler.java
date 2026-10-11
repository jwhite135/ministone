package com.minishiftsync.web;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		return problem(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new TreeMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		String detail = fieldErrors.entrySet().stream()
				.map(e -> e.getKey() + " " + e.getValue())
				.collect(Collectors.joining("; "));
		ResponseEntity<ProblemDetail> response = problem(HttpStatus.BAD_REQUEST, detail);
		response.getBody().setProperty("fieldErrors", fieldErrors);
		return response;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
		return problem(HttpStatus.BAD_REQUEST,
				"Request body is not valid JSON, or a value has the wrong format (dates are yyyy-MM-dd, times are HH:mm).");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "': " + ex.getValue());
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

}
