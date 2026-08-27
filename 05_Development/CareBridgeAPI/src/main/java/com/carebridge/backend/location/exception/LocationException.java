package com.carebridge.backend.location.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LocationException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final String code;

	public LocationException(HttpStatus httpStatus, String code, String message) {
		super(message);
		this.httpStatus = httpStatus;
		this.code = code;
	}
}
