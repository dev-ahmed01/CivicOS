package com.civicos.common.web;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.civicos.auth.application.InvalidRefreshTokenException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> authenticationFailure(AuthenticationException exception, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.", request);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ResponseEntity<ApiError> invalidRefreshToken(InvalidRefreshTokenException exception, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", exception.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> accessDenied(AccessDeniedException exception, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not authorised to perform this action.", request);
	}

	@ExceptionHandler(NoSuchElementException.class)
	ResponseEntity<ApiError> notFound(NoSuchElementException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
	ResponseEntity<ApiError> invalidRequest(Exception exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request is invalid.", request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
		String requestId = CorrelationIdFilter.requestId(request);
		LOGGER.error("Unhandled API error requestId={}", requestId, exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred.", requestId));
	}

	private ResponseEntity<ApiError> error(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(new ApiError(code, message, CorrelationIdFilter.requestId(request)));
	}
}
