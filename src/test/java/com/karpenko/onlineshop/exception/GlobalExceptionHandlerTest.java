package com.karpenko.onlineshop.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - Error Page Routing")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private Model model;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        model = new ExtendedModelMap();
    }

    @Test
    @DisplayName("ResourceNotFoundException should return 404 view")
    void shouldReturn404ForResourceNotFound() {
        String view = handler.handleResourceNotFound(
                new ResourceNotFoundException("Not found"), model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Not found");
    }

    @Test
    @DisplayName("ProductOutOfStockException should return 409 view")
    void shouldReturn409ForOutOfStock() {
        String view = handler.handleOutOfStock(
                new ProductOutOfStockException("Out of stock"), model);

        assertThat(view).isEqualTo("error/409");
    }

    @Test
    @DisplayName("Generic Exception should return 500 view")
    void shouldReturn500ForGenericError() {
        String view = handler.handleGenericException(
                new RuntimeException("Unexpected"), model);

        assertThat(view).isEqualTo("error/500");
        assertThat(model.getAttribute("errorMessage"))
                .isEqualTo("An internal error occurred. Please try again later.");
    }
}