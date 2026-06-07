package com.karpenko.onlineshop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception für den Fall, dass eine angeforderte Ressource nicht existiert.
 * Führt automatisch zu einem HTTP 404 Status, wenn sie nicht global abgefangen wird.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}