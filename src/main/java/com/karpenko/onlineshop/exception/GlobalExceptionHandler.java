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

    @ExceptionHandler(CartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCartNotFoundException(CartNotFoundException ex, Model model) {
        log.warn("Warenkorb nicht gefunden: {}", ex.getMessage());
        model.addAttribute("errorMessage", "Ihr Warenkorb konnte nicht gefunden werden.");
        return "error/404";
    }

    @ExceptionHandler(ProductOutOfStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOutOfStock(ProductOutOfStockException ex, Model model) {
        log.warn("Nicht genügend Lagerbestand: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/409";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        log.warn("Ungültiger Zustand: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoHandlerFound(NoHandlerFoundException ex, Model model) {
        log.warn("Seite nicht gefunden: {}", ex.getRequestURL());
        return "error/404";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException ex) {
        // Stille Behandlung für statische Ressourcen (z. B. favicon.ico)
        log.debug("Statische Ressource nicht gefunden: {}", ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unerwarteter Fehler aufgetreten", ex);
        model.addAttribute("errorMessage",
                "Ein unerwarteter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.");
        return "error/500";
    }
}