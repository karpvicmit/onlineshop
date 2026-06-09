package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationstest für OrderService.
 * Verwendet das Profil „test“ mit einer In-Memory-H2-Datenbank.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@karpenko.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRole(Role.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setAddress("Test Address 123");
        testUser = userRepository.save(testUser);

        testProduct = new Product();
        testProduct.setName("Test Laptop");
        testProduct.setPrice(new BigDecimal("999.99"));
        testProduct.setStock(5);
        testProduct = productRepository.save(testProduct);

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart = cartRepository.save(testCart);

        CartItem item = new CartItem();
        item.setCart(testCart);
        item.setProduct(testProduct);
        item.setQuantity(2);
        cartItemRepository.save(item);
    }

    @Test
    void checkout_SuccessfulOrder() {
        Order order = orderService.checkout(testUser);

        assertNotNull(order);
        assertNotNull(order.getId());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(new BigDecimal("1999.98"), order.getTotalAmount()); // 2 * 999.99
        assertEquals(1, order.getItems().size());

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(3, updatedProduct.getStock()); // 5 - 2 = 3

        Cart updatedCart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        assertTrue(updatedCart.getItems().isEmpty());
    }

    @Test
    void checkout_ProductOutOfStock_ThrowsException() {
        // GIVEN: Wir reduzieren den Produktbestand auf 1, aber es befinden sich noch 2 Stück im Warenkorb.
        testProduct.setStock(1);
        productRepository.save(testProduct);

        // WHEN & THEN: Wir erwarten eine Ausnahme.
        ProductOutOfStockException exception = assertThrows(
                ProductOutOfStockException.class,
                () -> orderService.checkout(testUser)
        );

        assertTrue(exception.getMessage().contains("Nicht genügend Lagerbestand"));

        // Wir prüfen, ob die Transaktion rückgängig gemacht wurde:
        // Der Lagerbestand hat sich nicht geändert,
        // die Bestellung wurde nicht erstellt.
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, updatedProduct.getStock());
        assertTrue(orderService.getOrderHistory(testUser).isEmpty());
    }
}