package com.ibb.onlineshop.exception;

/**
 * Custom Exception für den Fall, dass eine E-Mail-Adresse bereits registriert ist.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}