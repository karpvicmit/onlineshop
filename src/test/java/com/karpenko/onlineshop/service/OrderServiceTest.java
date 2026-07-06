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

    @Nested
    @DisplayName("checkout()")
    class Checkout {

        @Test
        @DisplayName("Should create order successfully without promo code")
        void shouldCheckoutSuccessfully() {
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

            Order result = orderService.checkout(user);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.NEW);
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

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cart is empty");
        }

        @Test
        @DisplayName("Should throw when cart not found")
        void shouldThrowWhenCartNotFound() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should throw when delivery address is missing")
        void shouldThrowWhenAddressMissing() {
            user.setAddress(null);

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Delivery address is missing");
        }

        @Test
        @DisplayName("Should throw when product stock is insufficient")
        void shouldThrowWhenStockInsufficient() {
            product.setStock(1); // only 1 in stock, but cart has 2
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(ProductOutOfStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

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

            Order result = orderService.checkout(user, "SAVE10");

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

            assertThatThrownBy(() -> orderService.checkout(user, "INVALID"))
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