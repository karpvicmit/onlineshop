package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.karpenko.onlineshop.service.PriceCalculatorService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Checkout & Status Management")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartService cartService;
    @Mock private CartRepository cartRepository;
    @Mock private PriceCalculatorService priceCalculatorService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setAddress("Berlin, Str. 1");

        product = new Product();
        product.setId(10L);
        product.setName("iPhone");
        product.setPrice(new BigDecimal("999.00"));
        product.setStock(5);

        cart = new Cart();
        cart.setUser(user);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);
        cart.addItem(item);
    }

    @Nested
    @DisplayName("checkout()")
    class Checkout {

        @Test
        @DisplayName("Should create order, decrease stock and clear cart")
        void shouldCheckoutSuccessfully() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(100L);
                return o;
            });

            Order result = orderService.checkout(user);

            // Stock decreased: 5 - 2 = 3
            assertThat(product.getStock()).isEqualTo(3);
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.NEW);
            assertThat(result.getTotalAmount()).isEqualByComparingTo("1998.00");
            assertThat(result.getDeliveryAddress()).isEqualTo("Berlin, Str. 1");
            assertThat(result.getOrderItems()).hasSize(1);

            OrderItem oi = result.getOrderItems().get(0);
            assertThat(oi.getUnitPrice()).isEqualByComparingTo("999.00");
            assertThat(oi.getQuantity()).isEqualTo(2);

            verify(cartService).clearCart(1L);
        }

        @Test
        @DisplayName("Should throw ProductOutOfStockException when stock < required")
        void shouldThrowWhenOutOfStock() {
            product.setStock(1); // less than requested 2
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(ProductOutOfStockException.class)
                    .hasMessageContaining("Insufficient stock");

            verify(orderRepository, never()).save(any());
            verify(cartService, never()).clearCart(anyLong());
        }

        @Test
        @DisplayName("Should throw when delivery address is missing")
        void shouldThrowWhenAddressMissing() {
            user.setAddress(null);

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("address");
        }

        @Test
        @DisplayName("Should throw when cart is empty")
        void shouldThrowWhenCartEmpty() {
            Cart emptyCart = new Cart();
            emptyCart.setUser(user);
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(emptyCart));

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("OrderItem should store unitPrice at checkout time")
        void shouldStoreUnitPriceAtOrderTime() {
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("1998.00"));
            Order result = orderService.checkout(user);

            // Simulate later price change
            product.setPrice(new BigDecimal("1299.00"));

            // OrderItem should still hold the original price
            assertThat(result.getOrderItems().get(0).getUnitPrice())
                    .isEqualByComparingTo("999.00");
        }
    }

    @Nested
    @DisplayName("updateOrderStatus()")
    class StatusTransitions {

        @Test
        @DisplayName("NEW -> SHIPPED via CONFIRMED (valid chain)")
        void shouldTransitionNewToConfirmed() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.NEW);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should reject invalid transition NEW -> SHIPPED (must go through CONFIRMED)")
        void shouldRejectInvalidTransition() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.NEW);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("Should not allow transitions from CANCELLED")
        void shouldNotTransitionFromCancelled() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should throw when order not found")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus(99L, OrderStatus.CONFIRMED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should preserve unitPrice in OrderItem even if product price changes later")
        void shouldPreserveUnitPriceAtCheckoutTime() {
            // given: product costs 100€ at checkout
            product.setPrice(new BigDecimal("100.00"));
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("200.00"));
            Order order = orderService.checkout(user);

            // when: product price changes to 200€ later
            product.setPrice(new BigDecimal("200.00"));

            // then: OrderItem still has original price
            assertThat(order.getOrderItems().get(0).getUnitPrice())
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should decrease stock atomically during checkout")
        void shouldDecreaseStockAtomically() {
            product.setStock(10);
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(3);
            cart.getItems().clear();
            cart.addItem(item);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(priceCalculatorService.calculateTotalWithLockedPrices(anyList(), anyMap()))
                    .thenReturn(new BigDecimal("300.00"));
            orderService.checkout(user);

            assertThat(product.getStock()).isEqualTo(7); // 10 - 3 = 7
        }

        @Test
        @DisplayName("Should throw when user has no delivery address")
        void shouldThrowWhenNoDeliveryAddress() {
            user.setAddress(null);

            assertThatThrownBy(() -> orderService.checkout(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("address");
        }
    }
}