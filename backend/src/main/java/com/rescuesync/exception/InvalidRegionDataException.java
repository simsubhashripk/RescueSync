package com.rescuesync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRegionDataException extends RuntimeException {
    public InvalidRegionDataException(String message) {
        super(message);
    }
}

