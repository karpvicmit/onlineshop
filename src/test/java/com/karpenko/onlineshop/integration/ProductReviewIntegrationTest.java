package com.karpenko.onlineshop.integration;

import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.*;
import com.karpenko.onlineshop.repository.*;
import com.karpenko.onlineshop.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductReview Integration - Full Review Flow")
class ProductReviewIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private ProductReviewService reviewService;

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private User buyer;
    private User nonBuyer;
    private Product product;

    @BeforeEach
    void setUp() {
        // Register and confirm buyer
        UserRegistrationDto buyerDto = new UserRegistrationDto();
        buyerDto.setEmail("buyer@test.de");
        buyerDto.setPassword("SecurePass123");
        buyerDto.setFirstName("Buyer");
        buyerDto.setLastName("User");
        buyerDto.setAddress("Buyer Street 1, 12345 Berlin");
        buyer = userService.registerUser(buyerDto);
        userService.confirmEmail(buyer.getEmailConfirmationToken());
        buyer = userRepository.findById(buyer.getId()).orElseThrow();

        // Register non-buyer
        UserRegistrationDto nonBuyerDto = new UserRegistrationDto();
        nonBuyerDto.setEmail("nonbuyer@test.de");
        nonBuyerDto.setPassword("SecurePass123");
        nonBuyerDto.setFirstName("Non");
        nonBuyerDto.setLastName("Buyer");
        nonBuyerDto.setAddress("Other Street 2, 54321 Munich");
        nonBuyer = userService.registerUser(nonBuyerDto);
        userService.confirmEmail(nonBuyer.getEmailConfirmationToken());
        nonBuyer = userRepository.findById(nonBuyer.getId()).orElseThrow();

        // Create product
        Category category = new Category();
        category.setName("Test Category");
        category = categoryService.saveCategory(category);

        product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(10);
        product.setCategory(category);
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("Full flow: purchase → submit review → moderate → verify stats")
    void fullReviewFlow() {
        // Step 1: Buyer purchases the product
        cartService.addItemToCart(buyer.getId(), product.getId(), 1);
        Order order = orderService.checkout(buyer);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);

        // Step 2: Buyer CAN write a review
        assertThat(reviewService.hasUserPurchasedAndNotReviewed(buyer.getId(), product.getId())).isTrue();

        // Step 3: Non-buyer CANNOT write a review
        assertThat(reviewService.hasUserPurchasedAndNotReviewed(nonBuyer.getId(), product.getId())).isFalse();
        assertThatThrownBy(() ->
                reviewService.addReview(nonBuyer.getId(), product.getId(), 5, "Title", "Comment"))
                .isInstanceOf(IllegalStateException.class);

        // Step 4: Buyer submits a review
        reviewService.addReview(buyer.getId(), product.getId(), 5, "Excellent!", "Best purchase ever.");

        // Step 5: Review is pending (not approved yet)
        Page<ProductReview> approvedBefore = reviewService.getApprovedReviews(
                product.getId(), PageRequest.of(0, 10));
        assertThat(approvedBefore.getContent()).isEmpty();

        // Step 6: Stats should be empty (no approved reviews)
        ReviewStatsDto statsBefore = reviewService.getReviewStats(product.getId());
        assertThat(statsBefore.getTotalCount()).isEqualTo(0L);

        // Step 7: Admin approves the review
        ProductReview pendingReview = reviewRepository.findAll().stream()
                .filter(r -> !r.isApproved())
                .findFirst()
                .orElseThrow();
        reviewService.approveReview(pendingReview.getId());

        // Step 8: Now review is visible
        Page<ProductReview> approvedAfter = reviewService.getApprovedReviews(
                product.getId(), PageRequest.of(0, 10));
        assertThat(approvedAfter.getContent()).hasSize(1);
        assertThat(approvedAfter.getContent().get(0).getTitle()).isEqualTo("Excellent!");

        // Step 9: Stats updated
        ReviewStatsDto statsAfter = reviewService.getReviewStats(product.getId());
        assertThat(statsAfter.getAverageRating()).isEqualTo(5.0);
        assertThat(statsAfter.getTotalCount()).isEqualTo(1L);

        // Step 10: Buyer cannot submit second review
        assertThat(reviewService.hasUserPurchasedAndNotReviewed(buyer.getId(), product.getId())).isFalse();
    }

    @Test
    @DisplayName("Admin can reject (delete) a review")
    void adminCanRejectReview() {
        // Setup: buyer purchases and submits review
        cartService.addItemToCart(buyer.getId(), product.getId(), 1);
        orderService.checkout(buyer);
        reviewService.addReview(buyer.getId(), product.getId(), 2, "Bad", "Not good");

        ProductReview pending = reviewRepository.findAll().stream()
                .filter(r -> !r.isApproved())
                .findFirst()
                .orElseThrow();

        // Admin rejects
        reviewService.rejectReview(pending.getId());

        // Review is gone
        assertThat(reviewRepository.findById(pending.getId())).isEmpty();
    }

    @Test
    @DisplayName("Multiple approved reviews should calculate correct average")
    void multipleReviewsAverage() {
        // Simulate two buyers purchasing and reviewing
        User buyer2 = registerAndConfirm("buyer2@test.de", "Second", "Buyer");

        cartService.addItemToCart(buyer.getId(), product.getId(), 1);
        orderService.checkout(buyer);
        reviewService.addReview(buyer.getId(), product.getId(), 5, "Great", "Love it");

        // Need fresh product stock for second buyer
        product.setStock(10);
        productRepository.save(product);

        cartService.addItemToCart(buyer2.getId(), product.getId(), 1);
        orderService.checkout(buyer2);
        reviewService.addReview(buyer2.getId(), product.getId(), 3, "Okay", "It's fine");

        // Approve both
        reviewRepository.findAll().stream()
                .filter(r -> !r.isApproved())
                .forEach(r -> reviewService.approveReview(r.getId()));

        // Average should be (5+3)/2 = 4.0
        ReviewStatsDto stats = reviewService.getReviewStats(product.getId());
        assertThat(stats.getAverageRating()).isEqualTo(4.0);
        assertThat(stats.getTotalCount()).isEqualTo(2L);
    }

    // ===== Helper =====

    private User registerAndConfirm(String email, String firstName, String lastName) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail(email);
        dto.setPassword("SecurePass123");
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setAddress("Address for " + email);
        User user = userService.registerUser(dto);
        userService.confirmEmail(user.getEmailConfirmationToken());
        return userRepository.findById(user.getId()).orElseThrow();
    }
}