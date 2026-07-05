package com.karpenko.onlineshop.integration;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.repository.*;
import com.karpenko.onlineshop.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Full Flow Integration Tests")
class FullFlowIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private FavoriteService favoriteService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("integration@test.de");
        dto.setPassword("SecurePass123");
        dto.setFirstName("Integration");
        dto.setLastName("Test");
        dto.setAddress("Test Street 1, 12345 Berlin");

        testUser = userService.registerUser(dto);

        // Confirming the email for the test user
        String token = testUser.getEmailConfirmationToken();
        userService.confirmEmail(token);

        // Reload the user from the database after confirmation.
        testUser = userRepository.findById(testUser.getId()).orElseThrow();

        Category category = new Category();
        category.setName("Test Category");
        category = categoryService.saveCategory(category);

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct.setCategory(category);
        testProduct.setSku("FULLFLOW-" + System.nanoTime());
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Complete shopping flow: register → confirm email → add to cart → checkout → verify order")
    void completeShoppingFlow() {
        // Step 1: Add product to cart
        cartService.addItemToCart(testUser.getId(), testProduct.getId(), 2);

        // Step 2: Verify cart
        var cartDto = cartService.getCartDtoForUser(testUser.getId());
        assertThat(cartDto.getItems()).hasSize(1);
        assertThat(cartDto.getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(cartDto.getItems().getFirst().getProductName()).isEqualTo("Test Product");
        assertThat(cartDto.getItemCount()).isEqualTo(2);
        assertThat(cartDto.getTotal()).isEqualByComparingTo("199.98");

        // Step 3: Checkout
        Order order = orderService.checkout(testUser);

        // Step 4: Verify order
        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("199.98");
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().getFirst().getUnitPrice()).isEqualByComparingTo("99.99");
        assertThat(order.getOrderItems().getFirst().getQuantity()).isEqualTo(2);

        // Step 5: Verify stock decreased
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(8);

        // Step 6: Verify cart is empty
        var cartAfterCheckout = cartService.getCartDtoForUser(testUser.getId());
        assertThat(cartAfterCheckout.getItems()).isEmpty();
        assertThat(cartAfterCheckout.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);

        // Step 7: Verify order in history
        var history = orderService.getOrderHistory(testUser);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getId()).isEqualTo(order.getId());
    }

    @Test
    @DisplayName("Favorite toggle: add and remove without duplicates")
    void favoriteToggleFlow() {
        favoriteService.toggleFavorite(testUser, testProduct.getId());
        assertThat(favoriteService.getFavoritesByUserId(testUser.getId())).hasSize(1);

        favoriteService.toggleFavorite(testUser, testProduct.getId());
        assertThat(favoriteService.getFavoritesByUserId(testUser.getId())).isEmpty();

        favoriteService.toggleFavorite(testUser, testProduct.getId());
        assertThat(favoriteService.getFavoritesByUserId(testUser.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Admin workflow: create category → create product → soft delete")
    void adminWorkflow() {
        Category newCategory = new Category();
        newCategory.setName("Electronics");
        newCategory = categoryService.saveCategory(newCategory);
        assertThat(newCategory.getSlug()).isEqualTo("electronics");

        Product newProduct = new Product();
        newProduct.setName("Smartphone");
        newProduct.setPrice(new BigDecimal("599.00"));
        newProduct.setStock(20);
        newProduct.setCategory(newCategory);
        newProduct.setSku("ADMIN-WF-" + System.nanoTime());
        newProduct = productRepository.save(newProduct);

        productService.deleteProduct(newProduct.getId());

        Product deletedProduct = productRepository.findById(newProduct.getId()).orElseThrow();
        assertThat(deletedProduct.isDeleted()).isTrue();
        assertThat(productRepository.findActiveById(newProduct.getId())).isEmpty();
    }
}