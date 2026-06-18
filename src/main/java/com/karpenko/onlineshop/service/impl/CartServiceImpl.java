package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.CartDto;
import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.exception.CartNotFoundException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.mapper.CartMapper;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.CartService;
import com.karpenko.onlineshop.service.PriceCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PriceCalculatorService priceCalculatorService;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartDto getCartDtoForUser(Long userId) {
        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        BigDecimal total = priceCalculatorService.calculateTotal(cart);
        log.debug("Cart loaded for user {}, total: {}", userId, total);
        return cartMapper.toDto(cart, total);
    }

    @Override
    @Transactional
    public void addItemToCart(Long userId, Long productId, Integer quantity) {
        log.info("Adding product {} (qty: {}) to cart for user {}", productId, quantity, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));

        Cart cart = cartRepository.findWithItemsByUserId(userId).orElseGet(() -> {
            log.info("Cart not found for user {}, creating new one", userId);
            Cart newCart = new Cart();
            newCart.setUser(userRepository.getReferenceById(userId));
            return cartRepository.save(newCart);
        });

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        int newQuantity;
        if (existingItem != null) {
            newQuantity = existingItem.getQuantity() + quantity;
        } else {
            newQuantity = quantity;
        }

        if (product.getStock() < newQuantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.addItem(newItem);
        }

        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void updateItemQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity <= 0) {
            removeItemFromCart(userId, productId);
            return;
        }

        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));

        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void removeItemFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));

        cart.removeItem(itemToRemove);
        cartRepository.save(cart);
        log.info("Product {} removed from cart for user {}", productId, userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findWithItemsByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Cart cleared for user {}", userId);
        });
    }
}