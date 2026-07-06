package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;
import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.repository.PromoCodeUsageRepository;
import com.karpenko.onlineshop.service.CartService;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.PriceCalculatorService;
import com.karpenko.onlineshop.service.PromoCodeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final PriceCalculatorService priceCalculatorService;
    private final PromoCodeService promoCodeService;
    private final PromoCodeUsageRepository promoCodeUsageRepository;

    @Override
    @Transactional
    public Order checkout(User user) {
        return checkout(user, null);
    }

    @Override
    @Transactional
    public Order checkout(User user, String promoCode) {
        log.info("Starting checkout for user: {} with promo code: {}",
                user.getEmail(), promoCode != null ? "***" : "none");

        if (user.getAddress() == null || user.getAddress().isBlank()) {
            throw new IllegalStateException("Delivery address is missing. Please update your profile.");
        }

        Cart cart = cartRepository.findWithItemsByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Cart is empty or not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot place order.");
        }

        // Lock products atomically
        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = productIds.stream()
                .map(id -> productRepository.findByIdWithLock(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id)))
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Validate stock
        for (CartItem cartItem : cart.getItems()) {
            Product lockedProduct = lockedProducts.get(cartItem.getProduct().getId());
            if (lockedProduct.getStock() < cartItem.getQuantity()) {
                log.warn("Insufficient stock for product: {} (available: {}, required: {})",
                        lockedProduct.getName(), lockedProduct.getStock(), cartItem.getQuantity());
                throw new ProductOutOfStockException(
                        "Insufficient stock for product: " + lockedProduct.getName());
            }
        }

        // Calculate subtotal with locked prices
        BigDecimal subtotal = priceCalculatorService
                .calculateTotalWithLockedPrices(cart.getItems(), lockedProducts);

        // Validate and apply promo code
        BigDecimal discountAmount = BigDecimal.ZERO;
        PromoCode appliedPromoCode = null;

        if (promoCode != null && !promoCode.isBlank()) {
            PromoCodeValidationResult promoResult =
                    promoCodeService.validatePromoCode(promoCode, subtotal);

            if (promoResult.isValid()) {
                appliedPromoCode = promoResult.getPromoCode();
                discountAmount = promoResult.getDiscountAmount();
                log.info("Promo code {} applied. Discount: {} €",
                        appliedPromoCode.getCode(), discountAmount);
            } else {
                log.warn("Invalid promo code: {}. Error: {}", promoCode, promoResult.getErrorMessage());
                throw new IllegalStateException("Invalid promo code: " + promoResult.getErrorMessage());
            }
        }

        BigDecimal finalTotal = priceCalculatorService.calculateFinalTotal(subtotal, discountAmount);

        // Build Order
        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(user.getAddress());
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(finalTotal);
        order.setDiscountAmount(discountAmount);
        order.setPromoCode(appliedPromoCode);

        for (CartItem cartItem : cart.getItems()) {
            Product lockedProduct = lockedProducts.get(cartItem.getProduct().getId());
            lockedProduct.setStock(lockedProduct.getStock() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(lockedProduct);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(lockedProduct.getPrice());
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Increment promo code usage counter
        if (appliedPromoCode != null) {
            promoCodeService.incrementUsage(appliedPromoCode.getId());

            PromoCodeUsage usage = new PromoCodeUsage();
            usage.setPromoCode(appliedPromoCode);
            usage.setUser(user);
            usage.setOrder(savedOrder);
            usage.setDiscountAmount(discountAmount);
            promoCodeUsageRepository.save(usage);

            log.info("Promo code usage recorded for order {}", savedOrder.getId());
        }

        log.info("Order {} created successfully. Subtotal: {}, Discount: {}, Final: {}",
                savedOrder.getId(), subtotal, discountAmount, finalTotal);

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

        OrderStatus previousStatus = order.getStatus();

        if (!isValidStatusTransition(previousStatus, newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + previousStatus + " -> " + newStatus);
        }

        // If cancelling an order with a promo code — decrement usage
        if (newStatus == OrderStatus.CANCELLED
                && previousStatus != OrderStatus.CANCELLED
                && order.getPromoCode() != null) {
            promoCodeService.decrementUsage(order.getPromoCode().getId());
            log.info("Promo code usage decremented due to order cancellation: order {}", orderId);
        }

        order.setStatus(newStatus);
        log.info("Order {} status updated to {}", orderId, newStatus);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus target) {
        return switch (current) {
            case NEW -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.CANCELLED;
            case CANCELLED -> false;
        };
    }
}