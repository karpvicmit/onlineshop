package com.karpenko.onlineshop.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Ressource nicht gefunden: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(ProductOutOfStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOutOfStock(ProductOutOfStockException ex, Model model) {
        log.warn("Nicht genügend Lagerbestand: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/409";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Unerwarteter Fehler aufgetreten", ex);
        model.addAttribute("errorMessage", "Ein unerwarteter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.");
        return "error/500";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoHandlerFound(NoHandlerFoundException ex, Model model) {
        log.warn("Seite nicht gefunden: {}", ex.getRequestURL());
        return "error/404";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("Statische Ressource nicht gefunden (wahrscheinlich favicon.ico): {}", ex.getMessage());
    }
}