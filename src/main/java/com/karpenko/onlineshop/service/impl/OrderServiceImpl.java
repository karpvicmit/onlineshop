package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.CartService;
import com.karpenko.onlineshop.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Override
    @Transactional
    public Order checkout(User user) {
        log.info("Starting checkout for user: {}", user.getEmail());

        if (user.getAddress() == null || user.getAddress().isBlank()) {
            throw new IllegalStateException("Delivery address is missing. Please update your profile.");
        }

        Cart cart = cartRepository.findWithItemsByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Cart is empty or not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot place order.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(user.getAddress());
        order.setStatus(OrderStatus.NEW);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            Product lockedProduct = productRepository.findByIdWithLock(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + product.getId()));

            if (lockedProduct.getStock() < cartItem.getQuantity()) {
                log.warn("Insufficient stock for product: {} (available: {}, required: {})",
                        lockedProduct.getName(), lockedProduct.getStock(), cartItem.getQuantity());
                throw new ProductOutOfStockException("Insufficient stock for product: " + lockedProduct.getName());
            }

            lockedProduct.setStock(lockedProduct.getStock() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(lockedProduct);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(lockedProduct.getPrice());

            order.addItem(orderItem);

            totalAmount = totalAmount.add(lockedProduct.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} created successfully. Total amount: {}", savedOrder.getId(), totalAmount);

        cartService.clearCart(user.getId());

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(User user) {
        return orderRepository.findByUserIdWithItems(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderDetails(Long orderId, User user) {
        return orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        log.debug("Fetching order by ID: {}", id);
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Order with ID " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderForAdmin(Long orderId) {
        log.debug("Fetching order for admin: {}", orderId);
        return orderRepository.findByIdWithUserAndItems(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with ID " + orderId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getAllOrdersForAdmin(Pageable pageable) {
        log.debug("Fetching orders for admin with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAllWithUserAndItems(pageable);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Admin updating order status: ID {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + order.getStatus() + " -> " + newStatus);
        }

        order.setStatus(newStatus);
        log.info("Order {} status updated to {}", orderId, newStatus);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus target) {
        switch (current) {
            case NEW:
                return target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED:
                return target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED:
                return target == OrderStatus.CANCELLED;
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
}