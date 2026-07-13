package com.karpenko.onlineshop.exception;

import com.karpenko.onlineshop.util.MessageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Error Page Routing & Localization")
class GlobalExceptionHandlerTest {

    @Mock
    private MessageUtil messageUtil;

    private GlobalExceptionHandler handler;
    private Model model;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageUtil);
        model = new ExtendedModelMap();

        // Default stubs for localized messages
        lenient().when(messageUtil.get(eq("error.cart.notFound")))
                .thenReturn("Your cart could not be found.");
        lenient().when(messageUtil.get(eq("error.data.conflict")))
                .thenReturn("Operation cannot be completed due to data conflict.");
        lenient().when(messageUtil.get(eq("error.internal")))
                .thenReturn("An internal error occurred. Please try again later.");
    }

    // ========================================================================
    // ResourceNotFoundException → 404
    // ========================================================================
    @Nested
    @DisplayName("ResourceNotFoundException → error/404")
    class ResourceNotFound {

        @Test
        @DisplayName("Should return 404 view with exception message")
        void shouldReturn404ForResourceNotFound() {
            String view = handler.handleResourceNotFound(
                    new ResourceNotFoundException("Product not found"), model);

            assertThat(view).isEqualTo("error/404");
            assertThat(model.getAttribute("errorMessage")).isEqualTo("Product not found");
        }

        @Test
        @DisplayName("Should pass original exception message (no localization needed)")
        void shouldPassOriginalMessage() {
            String view = handler.handleResourceNotFound(
                    new ResourceNotFoundException("Order #999 not found"), model);

            assertThat(view).isEqualTo("error/404");
            assertThat(model.getAttribute("errorMessage")).isEqualTo("Order #999 not found");
        }
    }

    // ========================================================================
    // CartNotFoundException → 404 (localized)
    // ========================================================================
    @Nested
    @DisplayName("CartNotFoundException → error/404 (localized)")
    class CartNotFound {

        @Test
        @DisplayName("Should return 404 view with localized message")
        void shouldReturn404WithLocalizedMessage() {
            String view = handler.handleCartNotFound(
                    new CartNotFoundException("Cart not found"), model);

            assertThat(view).isEqualTo("error/404");
            assertThat(model.getAttribute("errorMessage")).isEqualTo("Your cart could not be found.");
        }
    }

    // ========================================================================
    // ProductOutOfStockException → 409
    // ========================================================================
    @Nested
    @DisplayName("ProductOutOfStockException → error/409")
    class OutOfStock {

        @Test
        @DisplayName("Should return 409 view with exception message")
        void shouldReturn409ForOutOfStock() {
            String view = handler.handleOutOfStock(
                    new ProductOutOfStockException("Insufficient stock for product: iPhone"), model);

            assertThat(view).isEqualTo("error/409");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Insufficient stock for product: iPhone");
        }
    }

    // ========================================================================
    // EmailAlreadyExistsException → 409
    // ========================================================================
    @Nested
    @DisplayName("EmailAlreadyExistsException → error/409")
    class EmailExists {

        @Test
        @DisplayName("Should return 409 view with exception message")
        void shouldReturn409ForEmailExists() {
            String view = handler.handleEmailExists(
                    new EmailAlreadyExistsException("This email is already registered."), model);

            assertThat(view).isEqualTo("error/409");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("This email is already registered.");
        }
    }

    // ========================================================================
    // DataIntegrityViolationException → 409 (localized)
    // ========================================================================
    @Nested
    @DisplayName("DataIntegrityViolationException → error/409 (localized)")
    class DataIntegrity {

        @Test
        @DisplayName("Should return 409 view with localized message")
        void shouldReturn409WithLocalizedMessage() {
            String view = handler.handleDataIntegrity(
                    new DataIntegrityViolationException("Duplicate entry"), model);

            assertThat(view).isEqualTo("error/409");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("Operation cannot be completed due to data conflict.");
        }
    }

    // ========================================================================
    // Generic Exception → 500 (localized)
    // ========================================================================
    @Nested
    @DisplayName("Generic Exception → error/500 (localized)")
    class GenericError {

        @Test
        @DisplayName("Should return 500 view with localized message")
        void shouldReturn500ForGenericError() {
            String view = handler.handleGenericException(
                    new RuntimeException("Unexpected"), model);

            assertThat(view).isEqualTo("error/500");
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("An internal error occurred. Please try again later.");
        }

        @Test
        @DisplayName("Should NOT expose internal exception details to user")
        void shouldNotExposeInternalDetails() {
            String view = handler.handleGenericException(
                    new NullPointerException("secret internal stack trace"), model);

            assertThat(view).isEqualTo("error/500");
            assertThat((String) model.getAttribute("errorMessage"))
                    .doesNotContain("NullPointerException")
                    .doesNotContain("secret internal stack trace");
        }
    }
}