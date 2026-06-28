package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.entity.ProductReview;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.OrderItemRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.repository.ProductReviewRepository;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.impl.ProductReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewService - Review Business Logic")
class ProductReviewServiceTest {

    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductReviewServiceImpl reviewService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setFirstName("Max");
        user.setLastName("Mustermann");

        product = new Product();
        product.setId(10L);
        product.setName("iPhone 15");
    }

    @Nested
    @DisplayName("addReview()")
    class AddReview {

        @Test
        @DisplayName("Should save review with approved=false when user purchased product")
        void shouldSaveReviewAsPending() {
            // given
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(true);
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(productRepository.findActiveById(10L)).thenReturn(Optional.of(product));
            when(reviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            reviewService.addReview(1L, 10L, 5, "Great product", "Really love it!");

            // then
            ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
            verify(reviewRepository).save(captor.capture());

            ProductReview saved = captor.getValue();
            assertThat(saved.getRating()).isEqualTo(5);
            assertThat(saved.getTitle()).isEqualTo("Great product");
            assertThat(saved.getComment()).isEqualTo("Really love it!");
            assertThat(saved.isApproved()).isFalse(); // MUST be pending by default
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("Should throw when user has NOT purchased the product")
        void shouldThrowWhenUserDidNotPurchase() {
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(false);

            assertThatThrownBy(() ->
                    reviewService.addReview(1L, 10L, 5, "Title", "Comment"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("purchased");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when user already reviewed the product")
        void shouldThrowWhenAlreadyReviewed() {
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(true);
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

            assertThatThrownBy(() ->
                    reviewService.addReview(1L, 10L, 4, "Title", "Comment"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("purchased");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product is deleted")
        void shouldThrowWhenProductDeleted() {
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(true);
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(productRepository.findActiveById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reviewService.addReview(1L, 10L, 5, "Title", "Comment"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    @Nested
    @DisplayName("getReviewStats()")
    class ReviewStats {

        @Test
        @DisplayName("Should return average rating and count")
        void shouldReturnStats() {
            // ✅ Никаких Object[]! Просто мокруем возврат DTO
            doReturn(Optional.of(new ReviewStatsDto(4.5, 20L)))
                    .when(reviewRepository).findReviewStatsByProductId(10L);

            ReviewStatsDto stats = reviewService.getReviewStats(10L);

            assertThat(stats.getAverageRating()).isEqualTo(4.5);
            assertThat(stats.getTotalCount()).isEqualTo(20L);
        }

        @Test
        @DisplayName("Should return zeros when no reviews exist")
        void shouldReturnZerosWhenNoReviews() {
            // Репозиторий сам вернет 0.0 благодаря COALESCE, но если вернул пустой Optional
            doReturn(Optional.empty())
                    .when(reviewRepository).findReviewStatsByProductId(10L);

            ReviewStatsDto stats = reviewService.getReviewStats(10L);

            assertThat(stats.getAverageRating()).isEqualTo(0.0);
            assertThat(stats.getTotalCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("hasUserPurchasedAndNotReviewed()")
    class CanWriteReview {

        @Test
        @DisplayName("Should return true when user purchased and hasn't reviewed")
        void shouldReturnTrueWhenCanReview() {
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(true);
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);

            assertThat(reviewService.hasUserPurchasedAndNotReviewed(1L, 10L)).isTrue();
        }

        @Test
        @DisplayName("Should return false when user did NOT purchase")
        void shouldReturnFalseWhenNotPurchased() {
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(false);

            assertThat(reviewService.hasUserPurchasedAndNotReviewed(1L, 10L)).isFalse();
        }

        @Test
        @DisplayName("Should return false when user already reviewed")
        void shouldReturnFalseWhenAlreadyReviewed() {
            when(orderItemRepository.hasUserPurchasedProduct(1L, 10L)).thenReturn(true);
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

            assertThat(reviewService.hasUserPurchasedAndNotReviewed(1L, 10L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Moderation (approve / reject)")
    class Moderation {

        @Test
        @DisplayName("Should set approved=true when admin approves review")
        void shouldApproveReview() {
            ProductReview review = new ProductReview();
            review.setId(100L);
            review.setApproved(false);
            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

            reviewService.approveReview(100L);

            assertThat(review.isApproved()).isTrue();
            verify(reviewRepository).save(review);
        }

        @Test
        @DisplayName("Should delete review when admin rejects")
        void shouldRejectAndDeleteReview() {
            reviewService.rejectReview(100L);

            verify(reviewRepository).deleteById(100L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review not found")
        void shouldThrowWhenReviewNotFound() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.approveReview(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found");
        }
    }

    @Nested
    @DisplayName("getApprovedReviews()")
    class GetApprovedReviews {

        @Test
        @DisplayName("Should delegate to repository with pagination")
        void shouldReturnApprovedReviews() {
            ProductReview review = new ProductReview();
            review.setApproved(true);
            Page<ProductReview> page = new PageImpl<>(List.of(review));
            when(reviewRepository.findApprovedByProductId(eq(10L), any(PageRequest.class)))
                    .thenReturn(page);

            Page<ProductReview> result = reviewService.getApprovedReviews(10L, PageRequest.of(0, 5));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isApproved()).isTrue();
        }
    }
}