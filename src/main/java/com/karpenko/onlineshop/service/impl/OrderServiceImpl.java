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

    // === CHECKOUT OVERLOADS ===

    @Override
    @Transactional
    public Order checkout(User user) {
        return checkout(user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD);
    }

    @Override
    @Transactional
    public Order checkout(User user, String promoCode) {
        return checkout(user, promoCode, PaymentProvider.VORKASSE, ShippingMethod.STANDARD);
    }

    @Override
    @Transactional
    public Order checkout(User user, String promoCode, PaymentProvider paymentProvider) {
        return checkout(user, promoCode, paymentProvider, ShippingMethod.STANDARD);
    }

    @Override
    @Transactional
    public Order checkout(User user, String promoCode,
                          PaymentProvider paymentProvider,
                          ShippingMethod shippingMethod) {

        log.info("Starting checkout for user: {} | payment: {} | shipping: {}",
                user.getEmail(), paymentProvider, shippingMethod);

        if (paymentProvider == null) {
            throw new IllegalStateException("Payment provider is required");
        }
        if (shippingMethod == null) {
            throw new IllegalStateException("Shipping method is required");
        }
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
                throw new ProductOutOfStockException(
                        "Insufficient stock for product: " + lockedProduct.getName());
            }
        }

        BigDecimal subtotal = priceCalculatorService
                .calculateTotalWithLockedPrices(cart.getItems(), lockedProducts);

        // Promo code
        BigDecimal discountAmount = BigDecimal.ZERO;
        PromoCode appliedPromoCode = null;
        if (promoCode != null && !promoCode.isBlank()) {
            PromoCodeValidationResult promoResult =
                    promoCodeService.validatePromoCode(promoCode, subtotal);
            if (promoResult.isValid()) {
                appliedPromoCode = promoResult.getPromoCode();
                discountAmount = promoResult.getDiscountAmount();
            } else {
                throw new IllegalStateException("Invalid promo code: " + promoResult.getErrorMessage());
            }
        }

        // CORRECT LOGIC:
        // totalAmount = subtotal - discount  (WITHOUT shipping)
        // shippingCost is stored separately
        // grandTotal = totalAmount + shippingCost (computed by Order.getGrandTotal())
        BigDecimal finalTotal = priceCalculatorService
                .calculateFinalTotal(subtotal, discountAmount);

        BigDecimal shippingCost = shippingMethod.getCost();

        PaymentMethod paymentMethod = mapProviderToMethod(paymentProvider);

        // Build Order
        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(user.getAddress());
        order.setStatus(OrderStatus.NEW);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentProvider(paymentProvider);
        order.setShippingMethod(shippingMethod);
        order.setShippingCost(shippingCost);
        order.setTotalAmount(finalTotal);        // ← WITHOUT shipping
        order.setDiscountAmount(discountAmount);
        order.setPromoCode(appliedPromoCode);

        // For Rechnung: payment status is PENDING until customer pays the invoice
        if (paymentProvider == PaymentProvider.RECHNUNG) {
            order.setPaymentStatus(PaymentStatus.PENDING);
        }

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

        if (appliedPromoCode != null) {
            promoCodeService.incrementUsage(appliedPromoCode.getId());

            PromoCodeUsage usage = new PromoCodeUsage();
            usage.setPromoCode(appliedPromoCode);
            usage.setUser(user);
            usage.setOrder(savedOrder);
            usage.setDiscountAmount(discountAmount);
            promoCodeUsageRepository.save(usage);
        }

        log.info("Order {} created. Subtotal: {}, Shipping: {}, Discount: {}, Total: {}, GrandTotal: {}",
                savedOrder.getId(), subtotal, shippingCost, discountAmount,
                savedOrder.getTotalAmount(), savedOrder.getGrandTotal());

        cartService.clearCart(user.getId());

        return savedOrder;
    }

    private PaymentMethod mapProviderToMethod(PaymentProvider provider) {
        return switch (provider) {
            case STRIPE -> PaymentMethod.KREDITKARTE;
            case VORKASSE -> PaymentMethod.VORKASSE;
            case RECHNUNG -> PaymentMethod.RECHNUNG;
        };
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