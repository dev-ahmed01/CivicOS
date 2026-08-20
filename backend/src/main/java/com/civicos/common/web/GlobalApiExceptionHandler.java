package com.civicos.common.web;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.civicos.auth.application.InvalidRefreshTokenException;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.workflow.application.SeparationOfDutiesException;
import com.civicos.workflow.application.WorkflowPreconditionException;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;

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

	@ExceptionHandler(SeparationOfDutiesException.class)
	ResponseEntity<ApiError> separationOfDuties(
			SeparationOfDutiesException exception,
			HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "SEPARATION_OF_DUTIES_VIOLATION", exception.getMessage(), request);
	}

	@ExceptionHandler(WorkflowActionNotAllowedException.class)
	ResponseEntity<ApiError> workflowActionNotAllowed(
			WorkflowActionNotAllowedException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "WORKFLOW_ACTION_NOT_ALLOWED", exception.getMessage(), request);
	}

	@ExceptionHandler(WorkflowPreconditionException.class)
	ResponseEntity<ApiError> workflowPrecondition(
			WorkflowPreconditionException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "WORKFLOW_PRECONDITION_FAILED", exception.getMessage(), request);
	}

	@ExceptionHandler(DomainValidationException.class)
	ResponseEntity<ApiError> domainValidation(
			DomainValidationException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "DOMAIN_VALIDATION_FAILED", exception.getMessage(), request);
	}

	@ExceptionHandler(DomainConflictException.class)
	ResponseEntity<ApiError> domainConflict(
			DomainConflictException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "DOMAIN_CONFLICT", exception.getMessage(), request);
	}

	@ExceptionHandler({StaleEntityVersionException.class, OptimisticLockingFailureException.class})
	ResponseEntity<ApiError> concurrentModification(Exception exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
				"The resource was modified by another request.", request);
	}

	@ExceptionHandler(NoSuchElementException.class)
	ResponseEntity<ApiError> notFound(NoSuchElementException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			HandlerMethodValidationException.class,
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class,
			MissingRequestHeaderException.class
	})
	ResponseEntity<ApiError> invalidRequest(Exception exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request is invalid.", request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiError> uploadTooLarge(
			MaxUploadSizeExceededException exception,
			HttpServletRequest request) {
		return error(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE",
				"The uploaded file exceeds the configured limit.", request);
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
