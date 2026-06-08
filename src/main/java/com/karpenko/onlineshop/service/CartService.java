package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.User;

/**
 * Service-Interface für die Warenkorb-Geschäftslogik.
 * Alle Methoden erwarten das User-Objekt, um JPA-Beziehungen effizient zu verwalten.
 */
public interface CartService {

    Cart getOrCreateCartForUser(User user);

    void addItemToCart(User user, Long productId, Integer quantity);

    void updateItemQuantity(User user, Long productId, Integer quantity);

    void removeItemFromCart(User user, Long productId);

    void clearCart(User user);
}