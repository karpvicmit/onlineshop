package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.CartService;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.PriceCalculatorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final PriceCalculatorService priceCalculatorService;

    @Override
    @Transactional
    public Order checkout(User user) {
        log.info("Starte Checkout-Prozess für Benutzer: {}", user.getEmail());

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Warenkorb ist leer oder nicht gefunden"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Warenkorb ist leer. Keine Bestellung möglich.");
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                log.warn("Nicht genügend Lagerbestand für Produkt: {} (Verfügbar: {}, Benötigt: {})",
                        product.getName(), product.getStock(), item.getQuantity());
                throw new ProductOutOfStockException("Nicht genügend Lagerbestand für: " + product.getName());
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(user.getAddress());
        order.setStatus(OrderStatus.NEW);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            order.addItem(orderItem);

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Bestellung {} erfolgreich angelegt. Gesamtbetrag: {}", savedOrder.getId(), totalAmount);

        cartService.clearCart(user);

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(User user) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderDetails(Long orderId, User user) {
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bestellung nicht gefunden oder kein Zugriff"));
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        log.debug("Bestellung mit ID {} wird gesucht.", id);
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Bestellung mit ID " + id + " nicht gefunden"));
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Admin aktualisiert Bestellstatus: ID {} auf {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bestellung nicht gefunden"));
        order.setStatus(newStatus);
        orderRepository.save(order);
    }
    
}