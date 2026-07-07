package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;
import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.repository.PromoCodeUsageRepository;
import com.karpenko.onlineshop.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Order Management")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartService cartService;
    @Mock private CartRepository cartRepository;
    @Mock private PriceCalculatorService priceCalculatorService;
    @Mock private PromoCodeService promoCodeService;
    @Mock private PromoCodeUsageRepository promoCodeUsageRepository;

    private OrderServiceImpl orderService;
    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                productRepository,
                cartService,
                cartRepository,
                priceCalculatorService,
                promoCodeService,
                promoCodeUsageRepository
        );

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setAddress("Test Street 1, 12345 Berlin");

        product = new Product();
        product.setId(10L);
        product.setName("iPhone");
        product.setPrice(new BigDecimal("999.00"));
        product.setStock(5);

        cart = new Cart();
        cart.setUser(user);
        cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cart.addItem(cartItem);
    }

    // ========================================================================
    // checkout() — baseline scenarios
    // ========================================================================
    @Nested
    @DisplayName("checkout()")
    class Checkout {

        @Test
        @DisplayName("Should create order with VORKASSE and STANDARD shipping by default")
        void shouldCheckoutSuccessfully() {
            // given
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(priceCalculatorService.calculateFinalTotal(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("1998.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(100L);
                return o;
            });

            // when
            Order result = orderService.checkout(
                    user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.NEW);
            assertThat(result.getPaymentProvider()).isEqualTo(PaymentProvider.VORKASSE);
            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.VORKASSE);
            assertThat(result.getShippingMethod()).isEqualTo(ShippingMethod.STANDARD);
            assertThat(result.getShippingCost()).isEqualByComparingTo("4.99");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("1998.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPromoCode()).isNull();
            assertThat(product.getStock()).isEqualTo(3); // 5 - 2

            verify(cartService).clearCart(1L);
            verify(promoCodeService, never()).incrementUsage(any());
            verify(promoCodeUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when cart is empty")
        void shouldThrowWhenCartEmpty() {
            Cart emptyCart = new Cart();
            emptyCart.setUser(user);
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(emptyCart));

            assertThatThrownBy(() ->
                    orderService.checkout(user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cart is empty");
        }

        @Test
        @DisplayName("Should throw when cart not found")
        void shouldThrowWhenCartNotFound() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderService.checkout(user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should throw when delivery address is missing")
        void shouldThrowWhenAddressMissing() {
            user.setAddress(null);

            assertThatThrownBy(() ->
                    orderService.checkout(user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Delivery address is missing");
        }

        @Test
        @DisplayName("Should throw when product stock is insufficient")
        void shouldThrowWhenStockInsufficient() {
            product.setStock(1); // only 1 in stock, but cart has 2
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() ->
                    orderService.checkout(user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD))
                    .isInstanceOf(ProductOutOfStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderService.checkout(user, null, PaymentProvider.VORKASSE, ShippingMethod.STANDARD))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when payment provider is null")
        void shouldThrowWhenPaymentProviderNull() {
            assertThatThrownBy(() ->
                    orderService.checkout(user, null, null, ShippingMethod.STANDARD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Payment provider is required");
        }

        @Test
        @DisplayName("Should throw when shipping method is null")
        void shouldThrowWhenShippingMethodNull() {
            assertThatThrownBy(() ->
                    orderService.checkout(user, null, PaymentProvider.VORKASSE, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Shipping method is required");
        }
    }

    // ========================================================================
    // checkout() — delivery options and cost calculation
    // ========================================================================
    @Nested
    @DisplayName("checkout() — shipping methods")
    class CheckoutShipping {

        @Test
        @DisplayName("Should apply EXPRESS shipping cost (9.99 €)")
        void shouldApplyExpressShippingCost() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(priceCalculatorService.calculateFinalTotal(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("1998.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.checkout(
                    user, null, PaymentProvider.VORKASSE, ShippingMethod.EXPRESS);

            assertThat(result.getShippingMethod()).isEqualTo(ShippingMethod.EXPRESS);
            assertThat(result.getShippingCost()).isEqualByComparingTo("9.99");
        }

        @Test
        @DisplayName("Should apply ABHOLUNG shipping cost (0.00 €)")
        void shouldApplyAbholungShippingCost() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(priceCalculatorService.calculateFinalTotal(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("1998.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.checkout(
                    user, null, PaymentProvider.VORKASSE, ShippingMethod.ABHOLUNG);

            assertThat(result.getShippingMethod()).isEqualTo(ShippingMethod.ABHOLUNG);
            assertThat(result.getShippingCost()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("getGrandTotal() should include shipping cost")
        void shouldIncludeShippingInGrandTotal() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(priceCalculatorService.calculateFinalTotal(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("1998.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.checkout(
                    user, null, PaymentProvider.VORKASSE, ShippingMethod.EXPRESS);

            // grandTotal = totalAmount (1998.00) + shippingCost (9.99) = 2007.99
            assertThat(result.getGrandTotal()).isEqualByComparingTo("2007.99");
        }
    }

    // ========================================================================
    // checkout() — Rechnung
    // ========================================================================
    @Nested
    @DisplayName("checkout() — Rechnung payment")
    class CheckoutRechnung {

        @Test
        @DisplayName("Should set paymentStatus=PENDING for Rechnung orders")
        void shouldSetPendingStatusForRechnung() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(priceCalculatorService.calculateFinalTotal(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("1998.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.checkout(
                    user, null, PaymentProvider.RECHNUNG, ShippingMethod.STANDARD);

            assertThat(result.getPaymentProvider()).isEqualTo(PaymentProvider.RECHNUNG);
            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.RECHNUNG);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should map RECHNUNG provider to RECHNUNG method")
        void shouldMapRechnungProviderToMethod() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("100.00"));
            when(priceCalculatorService.calculateFinalTotal(any(), any()))
                    .thenReturn(new BigDecimal("100.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.checkout(
                    user, null, PaymentProvider.RECHNUNG, ShippingMethod.STANDARD);

            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.RECHNUNG);
        }
    }

    // ========================================================================
    // checkout() — Stripe
    // ========================================================================
    @Nested
    @DisplayName("checkout() — Stripe payment")
    class CheckoutStripe {

        @Test
        @DisplayName("Should map STRIPE provider to KREDITKARTE method")
        void shouldMapStripeToKreditkarte() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("100.00"));
            when(priceCalculatorService.calculateFinalTotal(any(), any()))
                    .thenReturn(new BigDecimal("100.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.checkout(
                    user, null, PaymentProvider.STRIPE, ShippingMethod.STANDARD);

            assertThat(result.getPaymentProvider()).isEqualTo(PaymentProvider.STRIPE);
            assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.KREDITKARTE);
        }
    }

    // ========================================================================
    // checkout() — promo code
    // ========================================================================
    @Nested
    @DisplayName("checkout() with promo code")
    class CheckoutWithPromoCode {

        @Test
        @DisplayName("Should apply valid promo code and create PromoCodeUsage")
        void shouldApplyPromoCodeAndCreateUsage() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(priceCalculatorService.calculateFinalTotal(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("1798.20"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(100L);
                return o;
            });

            PromoCode promoCode = new PromoCode();
            promoCode.setId(1L);
            promoCode.setCode("SAVE10");

            PromoCodeValidationResult promoResult = PromoCodeValidationResult.builder()
                    .valid(true)
                    .promoCode(promoCode)
                    .discountAmount(new BigDecimal("199.80"))
                    .finalTotal(new BigDecimal("1798.20"))
                    .build();

            when(promoCodeService.validatePromoCode(eq("SAVE10"), any(BigDecimal.class)))
                    .thenReturn(promoResult);

            Order result = orderService.checkout(
                    user, "SAVE10", PaymentProvider.VORKASSE, ShippingMethod.STANDARD);

            assertThat(result).isNotNull();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("199.80");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("1798.20");
            assertThat(result.getPromoCode()).isEqualTo(promoCode);

            verify(promoCodeService).incrementUsage(1L);
            verify(promoCodeUsageRepository).save(any());
        }

        @Test
        @DisplayName("Should throw when promo code is invalid")
        void shouldThrowWhenPromoCodeInvalid() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));

            PromoCodeValidationResult promoResult = PromoCodeValidationResult.failure("Invalid code");
            when(promoCodeService.validatePromoCode(eq("INVALID"), any(BigDecimal.class)))
                    .thenReturn(promoResult);

            assertThatThrownBy(() ->
                    orderService.checkout(user, "INVALID", PaymentProvider.VORKASSE, ShippingMethod.STANDARD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid promo code");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should decrement promo usage when order is cancelled")
        void shouldDecrementPromoUsageOnCancel() {
            PromoCode promoCode = new PromoCode();
            promoCode.setId(1L);

            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.NEW);
            order.setPromoCode(promoCode);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

            verify(promoCodeService).decrementUsage(1L);
        }
    }

    // ========================================================================
    // getOrderHistory()
    // ========================================================================
    @Nested
    @DisplayName("getOrderHistory()")
    class GetOrderHistory {

        @Test
        @DisplayName("Should return order history for user")
        void shouldReturnOrderHistory() {
            Order order1 = new Order();
            order1.setId(1L);
            Order order2 = new Order();
            order2.setId(2L);

            when(orderRepository.findByUserIdWithItems(1L)).thenReturn(List.of(order1, order2));

            List<Order> result = orderService.getOrderHistory(user);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Order::getId).containsExactly(1L, 2L);
        }
    }

    // ========================================================================
    // getOrderDetails()
    // ========================================================================
    @Nested
    @DisplayName("getOrderDetails()")
    class GetOrderDetails {

        @Test
        @DisplayName("Should return order details for user")
        void shouldReturnOrderDetails() {
            Order order = new Order();
            order.setId(1L);
            order.setUser(user);

            when(orderRepository.findByIdAndUserIdWithItems(1L, 1L)).thenReturn(Optional.of(order));

            Order result = orderService.getOrderDetails(1L, user);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw when order not found or access denied")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findByIdAndUserIdWithItems(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetails(99L, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // updateOrderStatus()
    // ========================================================================
    @Nested
    @DisplayName("updateOrderStatus()")
    class UpdateOrderStatus {

        @Test
        @DisplayName("Should update order status from NEW to CONFIRMED")
        void shouldUpdateStatusToConfirmed() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.NEW);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should throw when invalid status transition")
        void shouldThrowOnInvalidTransition() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.CANCELLED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("Should throw when order not found")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus(99L, OrderStatus.CONFIRMED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // Admin methods
    // ========================================================================
    @Nested
    @DisplayName("Admin methods")
    class AdminMethods {

        @Test
        @DisplayName("Should get order for admin")
        void shouldGetOrderForAdmin() {
            Order order = new Order();
            order.setId(1L);

            when(orderRepository.findByIdWithUserAndItems(1L)).thenReturn(Optional.of(order));

            Order result = orderService.getOrderForAdmin(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should get all orders for admin with pagination")
        void shouldGetAllOrdersForAdmin() {
            Order order1 = new Order();
            order1.setId(1L);
            Order order2 = new Order();
            order2.setId(2L);

            Page<Order> page = new PageImpl<>(List.of(order1, order2), PageRequest.of(0, 10), 2);
            when(orderRepository.findAllWithUserAndItems(any(PageRequest.class))).thenReturn(page);

            Page<Order> result = orderService.getAllOrdersForAdmin(PageRequest.of(0, 10));

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }
    }
}