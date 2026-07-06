package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.cart.CartDto;
import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;
import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.mapper.CartMapper;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService - Shopping Cart Logic")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PriceCalculatorService priceCalculatorService;
    @Mock private PromoCodeService promoCodeService;

    private CartMapper cartMapper;
    private CartServiceImpl cartService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        // Создаём реальный CartMapper с моком PriceCalculatorService
        cartMapper = new CartMapper(priceCalculatorService);

        // Создаём CartServiceImpl с реальным mapper
        cartService = new CartServiceImpl(
                cartRepository,
                productRepository,
                userRepository,
                cartMapper,
                promoCodeService,
                priceCalculatorService
        );

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");

        product = new Product();
        product.setId(10L);
        product.setName("iPhone");
        product.setPrice(new BigDecimal("999.00"));
        product.setStock(5);
    }

    @Nested
    @DisplayName("addItemToCart()")
    class AddItem {
        @Test
        @DisplayName("Should create cart and add product when cart does not exist")
        void shouldCreateCartIfMissing() {
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.getReferenceById(1L)).thenReturn(user);
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.addItemToCart(1L, 10L, 1);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository, times(2)).save(captor.capture());
            Cart saved = captor.getAllValues().get(1);
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getItems()).hasSize(1);
            assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(1);
            assertThat(saved.getItems().get(0).getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("Should increase quantity when product already in cart")
        void shouldIncreaseQuantityForExistingProduct() {
            Cart cart = new Cart();
            cart.setUser(user);
            CartItem existing = new CartItem();
            existing.setProduct(product);
            existing.setQuantity(2);
            cart.addItem(existing);

            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.addItemToCart(1L, 10L, 3);

            assertThat(existing.getQuantity()).isEqualTo(5);
            assertThat(cart.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw when requested quantity exceeds stock")
        void shouldThrowWhenStockInsufficient() {
            product.setStock(2);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.getReferenceById(1L)).thenReturn(user);
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> cartService.addItemToCart(1L, 10L, 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Should throw when product not found")
        void shouldThrowWhenProductMissing() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItemToCart(1L, 99L, 1))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateItemQuantity()")
    class UpdateQuantity {
        @Test
        @DisplayName("Should set new quantity for existing item")
        void shouldUpdateQuantity() {
            Cart cart = new Cart();
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(1);
            cart.addItem(item);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.updateItemQuantity(1L, 10L, 4);

            assertThat(item.getQuantity()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should remove item when quantity is 0 or negative")
        void shouldRemoveItemWhenQuantityZero() {
            Cart cart = new Cart();
            CartItem item = new CartItem();
            item.setProduct(product);
            cart.addItem(item);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.updateItemQuantity(1L, 10L, 0);

            assertThat(cart.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeItemFromCart()")
    class RemoveItem {
        @Test
        @DisplayName("Should remove product from cart")
        void shouldRemoveItem() {
            Cart cart = new Cart();
            CartItem item = new CartItem();
            item.setProduct(product);
            cart.addItem(item);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.removeItemFromCart(1L, 10L);

            assertThat(cart.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("clearCart()")
    class ClearCart {
        @Test
        @DisplayName("Should remove all items from cart")
        void shouldClearAllItems() {
            Cart cart = new Cart();
            CartItem i1 = new CartItem(); i1.setProduct(product);
            CartItem i2 = new CartItem(); i2.setProduct(product);
            cart.addItem(i1);
            cart.addItem(i2);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.clearCart(1L);

            assertThat(cart.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCartDtoForUser() with promo code")
    class GetCartWithPromoCode {

        @Test
        @DisplayName("Should apply valid promo code to cart")
        void shouldApplyValidPromoCode() {
            Cart cart = new Cart();
            cart.setUser(user);
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(2);
            cart.addItem(item);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(priceCalculatorService.calculateTotal(cart)).thenReturn(new BigDecimal("1998.00"));

            PromoCodeValidationResult promoResult = PromoCodeValidationResult.builder()
                    .valid(true)
                    .promoCode(buildPromoCode("SAVE10"))
                    .discountAmount(new BigDecimal("199.80"))
                    .finalTotal(new BigDecimal("1798.20"))
                    .build();

            when(promoCodeService.validatePromoCode(eq("SAVE10"), any(BigDecimal.class)))
                    .thenReturn(promoResult);

            CartDto result = cartService.getCartDtoForUser(1L, "SAVE10");

            assertThat(result).isNotNull();
            assertThat(result.getAppliedPromoCode()).isEqualTo("SAVE10");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("199.80");
            assertThat(result.getFinalTotal()).isEqualByComparingTo("1798.20");
        }

        @Test
        @DisplayName("Should return error message for invalid promo code")
        void shouldReturnErrorForInvalidPromoCode() {
            Cart cart = new Cart();
            cart.setUser(user);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(priceCalculatorService.calculateTotal(cart)).thenReturn(new BigDecimal("100.00"));

            PromoCodeValidationResult promoResult = PromoCodeValidationResult.failure("Invalid code");
            when(promoCodeService.validatePromoCode(eq("INVALID"), any(BigDecimal.class)))
                    .thenReturn(promoResult);

            CartDto result = cartService.getCartDtoForUser(1L, "INVALID");

            assertThat(result).isNotNull();
            assertThat(result.getAppliedPromoCode()).isNull();
            assertThat(result.getPromoErrorMessage()).isEqualTo("Invalid code");
        }

        @Test
        @DisplayName("Should return cart without promo when promoCode is null")
        void shouldReturnCartWithoutPromoWhenNull() {
            Cart cart = new Cart();
            cart.setUser(user);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(priceCalculatorService.calculateTotal(cart)).thenReturn(BigDecimal.ZERO);

            CartDto result = cartService.getCartDtoForUser(1L, null);

            assertThat(result).isNotNull();
            assertThat(result.getAppliedPromoCode()).isNull();
            assertThat(result.getPromoErrorMessage()).isNull();
            verify(promoCodeService, never()).validatePromoCode(any(), any());
        }

        @Test
        @DisplayName("Should return cart without promo when promoCode is blank")
        void shouldReturnCartWithoutPromoWhenBlank() {
            Cart cart = new Cart();
            cart.setUser(user);

            when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
            when(priceCalculatorService.calculateTotal(cart)).thenReturn(BigDecimal.ZERO);

            CartDto result = cartService.getCartDtoForUser(1L, "   ");

            assertThat(result).isNotNull();
            assertThat(result.getAppliedPromoCode()).isNull();
            verify(promoCodeService, never()).validatePromoCode(any(), any());
        }

        private PromoCode buildPromoCode(String code) {
            PromoCode promo = new PromoCode();
            promo.setId(1L);
            promo.setCode(code);
            promo.setDiscountType(DiscountType.PERCENTAGE);
            promo.setDiscountValue(new BigDecimal("10.00"));
            return promo;
        }
    }
}