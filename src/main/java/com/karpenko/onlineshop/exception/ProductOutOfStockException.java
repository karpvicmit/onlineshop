package com.karpenko.onlineshop.exception;

/**
 * Wird geworfen, wenn der Lagerbestand eines Produkts für die gewünschte Menge nicht ausreicht.
 * Führt zu einem Rollback der @Transactional Checkout-Methode.
 */
public class ProductOutOfStockException extends RuntimeException {
    public ProductOutOfStockException(String message) {
        super(message);
    }
}