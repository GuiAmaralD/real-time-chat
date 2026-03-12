package com.guiamaral.real_time_chat.controller;

import com.guiamaral.real_time_chat.dto.common.ErrorResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.getStatus())
				.body(new ErrorResponse(exception.getMessage()));
	}
}
