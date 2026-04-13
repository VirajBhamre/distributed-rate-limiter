package com.distributed.ratelimiter.exception;

import com.distributed.ratelimiter.config.AppInstanceProperties;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final AppInstanceProperties instanceProperties;

	public GlobalExceptionHandler(AppInstanceProperties instanceProperties) {
		this.instanceProperties = instanceProperties;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			errors.put(fe.getField(), fe.getDefaultMessage());
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("instanceId", instanceProperties.id());
		body.put("error", "validation_failed");
		body.put("fields", errors);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		Map<String, String> body = new LinkedHashMap<>();
		body.put("instanceId", instanceProperties.id());
		body.put("error", reason);
		return ResponseEntity.status(status).body(body);
	}
}
