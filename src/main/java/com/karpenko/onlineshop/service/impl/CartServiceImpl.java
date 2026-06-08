package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Cart getOrCreateCartForUser(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            log.info("Erstelle neuen Warenkorb für Benutzer: {}", user.getEmail());
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    @Override
    @Transactional
    public void addItemToCart(User user, Long productId, Integer quantity) {
        log.info("Füge Produkt {} (Menge: {}) zum Warenkorb von {} hinzu", productId, quantity, user.getEmail());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produkt mit ID " + productId + " nicht gefunden"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Nicht genügend Lagerbestand für Produkt: " + product.getName());
        }

        Cart cart = getOrCreateCartForUser(user);

        // Prüfen, ob das Produkt bereits im Warenkorb ist
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Menge erhöhen und Lagerbestand erneut prüfen
            int newQuantity = existingItem.getQuantity() + quantity;
            if (product.getStock() < newQuantity) {
                throw new IllegalStateException("Nicht genügend Lagerbestand für die gewünschte Gesamtmenge von: " + product.getName());
            }
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void updateItemQuantity(User user, Long productId, Integer quantity) {
        if (quantity <= 0) {
            removeItemFromCart(user, productId);
            return;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produkt mit ID " + productId + " nicht gefunden"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Nicht genügend Lagerbestand für Produkt: " + product.getName());
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Warenkorb nicht gefunden"));

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    cartRepository.save(cart);
                });
    }

    @Override
    @Transactional
    public void removeItemFromCart(User user, Long productId) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Warenkorb nicht gefunden"));

        boolean removed = cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        if (removed) {
            cartRepository.save(cart);
            log.info("Produkt {} aus dem Warenkorb von {} entfernt", productId, user.getEmail());
        }
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Warenkorb für Benutzer {} vollständig geleert", user.getEmail());
        });
    }
}