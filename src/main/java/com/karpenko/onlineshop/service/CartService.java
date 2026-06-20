package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.cart.CartDto;

public interface CartService {

    /**
     * Lädt den Warenkorb des Benutzers als DTO inkl. berechneter Gesamtsumme.
     */
    CartDto getCartDtoForUser(Long userId);

    void addItemToCart(Long userId, Long productId, Integer quantity);

    void updateItemQuantity(Long userId, Long productId, Integer quantity);

    void removeItemFromCart(Long userId, Long productId);
    
    void clearCart(Long userId);
}