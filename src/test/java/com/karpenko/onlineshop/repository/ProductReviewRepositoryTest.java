package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductReviewRepository - JPQL Queries & Constraints")
class ProductReviewRepositoryTest {

    @Autowired private ProductReviewRepository reviewRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        // Очистка в правильном порядке (из-за FK constraints)
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Создаём пользователей
        user1 = createUser("user1@test.de", "Max", "Mustermann");
        user2 = createUser("user2@test.de", "Anna", "Schmidt");
        user3 = createUser("user3@test.de", "Tom", "Weber");

        // Создаём категории
        Category electronics = createCategory("Elektronik", "elektronik");
        Category books = createCategory("Bücher", "buecher");

        // Создаём продукты
        product1 = createProduct("iPhone 15", electronics, new BigDecimal("999.00"), 10);
        product2 = createProduct("Clean Code", books, new BigDecimal("35.50"), 20);
    }

    // ==========================================
    // findApprovedByProductId()
    // ==========================================
    @Nested
    @DisplayName("findApprovedByProductId()")
    class FindApprovedReviews {

        @Test
        @DisplayName("Should return only approved reviews, ordered by createdAt DESC")
        void shouldReturnOnlyApprovedReviews() {
            // given
            ProductReview approved1 = createReview(user1, product1, 5, "Great!", "Love it", true);
            createReview(user2, product1, 3, "Okay", "Not bad", false); // pending — должен быть исключён
            ProductReview approved2 = createReview(user3, product1, 4, "Good", "Nice product", true);

            entityManager.flush();

            // when
            Page<ProductReview> result = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .allMatch(ProductReview::isApproved)
                    .extracting(ProductReview::getId)
                    // DESC order: approved2 создан позже → первый
                    .containsExactly(approved2.getId(), approved1.getId());
        }

        @Test
        @DisplayName("Should return empty page when no approved reviews exist")
        void shouldReturnEmptyPageWhenNoApproved() {
            createReview(user1, product1, 5, "Pending 1", "Text", false);
            createReview(user2, product1, 3, "Pending 2", "Text", false);

            Page<ProductReview> result = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should not return reviews for other products")
        void shouldNotReturnReviewsForOtherProducts() {
            createReview(user1, product1, 5, "For product1", "Text", true);
            createReview(user2, product2, 4, "For product2", "Text", true);

            Page<ProductReview> result = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getProduct().getId()).isEqualTo(product1.getId());
        }

        @Test
        @DisplayName("Should support pagination")
        void shouldSupportPagination() {
            // Создаём 5 одобренных отзывов
            for (int i = 0; i < 5; i++) {
                User u = createUser("reviewer" + i + "@test.de", "User" + i, "Test");
                createReview(u, product1, 4, "Title " + i, "Comment " + i, true);
            }

            Page<ProductReview> page0 = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(0, 2));
            Page<ProductReview> page1 = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(1, 2));
            Page<ProductReview> page2 = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(2, 2));

            assertThat(page0.getContent()).hasSize(2);
            assertThat(page1.getContent()).hasSize(2);
            assertThat(page2.getContent()).hasSize(1);
            assertThat(page0.getTotalElements()).isEqualTo(5);
            assertThat(page0.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should eagerly fetch user to avoid LazyInitializationException")
        void shouldEagerlyFetchUser() {
            createReview(user1, product1, 5, "Title", "Comment", true);
            entityManager.flush();
            entityManager.clear(); // очищаем persistence context

            Page<ProductReview> result = reviewRepository.findApprovedByProductId(
                    product1.getId(), PageRequest.of(0, 10));

            // Если JOIN FETCH работает — обращение к user не вызовет LazyInitializationException
            assertThat(result.getContent().get(0).getUser().getEmail()).isEqualTo("user1@test.de");
        }
    }

    // ==========================================
    // findReviewStatsByProductId() — DTO-проекция
    // ==========================================
    @Nested
    @DisplayName("findReviewStatsByProductId()")
    class ReviewStats {

        @Test
        @DisplayName("Should calculate average rating and count correctly")
        void shouldCalculateStats() {
            createReview(user1, product1, 5, "Great", "Love it", true);
            createReview(user2, product1, 3, "Okay", "Not bad", true);

            Optional<ReviewStatsDto> result = reviewRepository.findReviewStatsByProductId(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getAverageRating()).isEqualTo(4.0); // (5+3)/2
            assertThat(result.get().getTotalCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should NOT include pending reviews in statistics")
        void shouldExcludePendingFromStats() {
            createReview(user1, product1, 5, "Great", "Love it", true);
            createReview(user2, product1, 1, "Bad", "Terrible", false); // pending

            Optional<ReviewStatsDto> result = reviewRepository.findReviewStatsByProductId(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getAverageRating()).isEqualTo(5.0); // только approved
            assertThat(result.get().getTotalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return 0.0 and 0 when no approved reviews exist (COALESCE)")
        void shouldReturnZerosWhenNoReviews() {
            Optional<ReviewStatsDto> result = reviewRepository.findReviewStatsByProductId(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getAverageRating()).isEqualTo(0.0);
            assertThat(result.get().getTotalCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should return 0.0 and 0 when only pending reviews exist")
        void shouldReturnZerosWhenOnlyPending() {
            createReview(user1, product1, 5, "Pending", "Text", false);
            createReview(user2, product1, 1, "Pending", "Text", false);

            Optional<ReviewStatsDto> result = reviewRepository.findReviewStatsByProductId(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getAverageRating()).isEqualTo(0.0);
            assertThat(result.get().getTotalCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle fractional average correctly")
        void shouldHandleFractionalAverage() {
            createReview(user1, product1, 5, "Great", "Text", true);
            createReview(user2, product1, 4, "Good", "Text", true);
            createReview(user3, product1, 3, "Okay", "Text", true);

            Optional<ReviewStatsDto> result = reviewRepository.findReviewStatsByProductId(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getAverageRating()).isEqualTo(4.0); // (5+4+3)/3
            assertThat(result.get().getTotalCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should calculate stats independently per product")
        void shouldCalculateStatsPerProduct() {
            createReview(user1, product1, 5, "For P1", "Text", true);
            createReview(user2, product2, 2, "For P2", "Text", true);

            Optional<ReviewStatsDto> stats1 = reviewRepository.findReviewStatsByProductId(product1.getId());
            Optional<ReviewStatsDto> stats2 = reviewRepository.findReviewStatsByProductId(product2.getId());

            assertThat(stats1.get().getAverageRating()).isEqualTo(5.0);
            assertThat(stats2.get().getAverageRating()).isEqualTo(2.0);
        }
    }

    // ==========================================
    // existsByUserIdAndProductId()
    // ==========================================
    @Nested
    @DisplayName("existsByUserIdAndProductId()")
    class ExistsReview {

        @Test
        @DisplayName("Should return true when user has reviewed product")
        void shouldReturnTrueWhenExists() {
            createReview(user1, product1, 5, "Title", "Comment", true);

            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product1.getId())).isTrue();
        }

        @Test
        @DisplayName("Should return true even for pending review")
        void shouldReturnTrueForPendingReview() {
            createReview(user1, product1, 5, "Title", "Comment", false);

            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product1.getId())).isTrue();
        }

        @Test
        @DisplayName("Should return false when user has NOT reviewed product")
        void shouldReturnFalseWhenNotExists() {
            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product1.getId())).isFalse();
        }

        @Test
        @DisplayName("Should distinguish between different users for same product")
        void shouldDistinguishUsers() {
            createReview(user1, product1, 5, "Title", "Comment", true);

            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product1.getId())).isTrue();
            assertThat(reviewRepository.existsByUserIdAndProductId(user2.getId(), product1.getId())).isFalse();
        }

        @Test
        @DisplayName("Should distinguish between different products for same user")
        void shouldDistinguishProducts() {
            createReview(user1, product1, 5, "Title", "Comment", true);

            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product1.getId())).isTrue();
            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product2.getId())).isFalse();
        }
    }

    // ==========================================
    // findAllPending()
    // ==========================================
    @Nested
    @DisplayName("findAllPending()")
    class FindPendingReviews {

        @Test
        @DisplayName("Should return only unapproved reviews")
        void shouldReturnOnlyPending() {
            createReview(user1, product1, 5, "Approved", "Text", true);
            ProductReview pending1 = createReview(user2, product1, 3, "Pending 1", "Text", false);
            ProductReview pending2 = createReview(user3, product2, 4, "Pending 2", "Text", false);

            List<ProductReview> pending = reviewRepository.findAllPending();

            assertThat(pending).hasSize(2);
            assertThat(pending)
                    .allMatch(r -> !r.isApproved())
                    .extracting(ProductReview::getId)
                    .containsExactlyInAnyOrder(pending1.getId(), pending2.getId());
        }

        @Test
        @DisplayName("Should return empty list when all reviews are approved")
        void shouldReturnEmptyWhenAllApproved() {
            createReview(user1, product1, 5, "Approved", "Text", true);
            createReview(user2, product1, 4, "Approved", "Text", true);

            List<ProductReview> pending = reviewRepository.findAllPending();

            assertThat(pending).isEmpty();
        }

        @Test
        @DisplayName("Should eagerly fetch user and product")
        void shouldEagerlyFetchRelations() {
            createReview(user1, product1, 5, "Title", "Comment", false);
            entityManager.flush();
            entityManager.clear();

            List<ProductReview> pending = reviewRepository.findAllPending();

            // Если JOIN FETCH работает — обращения к связям не вызовут LazyInitializationException
            assertThat(pending.get(0).getUser().getEmail()).isEqualTo("user1@test.de");
            assertThat(pending.get(0).getProduct().getName()).isEqualTo("iPhone 15");
        }

        @Test
        @DisplayName("Should order pending reviews by createdAt ASC (oldest first)")
        void shouldOrderByCreatedAtAsc() {
            ProductReview older = createReview(user1, product1, 5, "Older", "Text", false);
            // Небольшая задержка для гарантии разного createdAt
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            ProductReview newer = createReview(user2, product1, 4, "Newer", "Text", false);

            List<ProductReview> pending = reviewRepository.findAllPending();

            assertThat(pending).hasSize(2);
            assertThat(pending.get(0).getId()).isEqualTo(older.getId());
            assertThat(pending.get(1).getId()).isEqualTo(newer.getId());
        }
    }

    // ==========================================
    // UNIQUE constraint (user_id, product_id)
    // ==========================================
    @Nested
    @DisplayName("UNIQUE constraint (user_id, product_id)")
    class UniqueConstraint {

        @Test
        @DisplayName("Should enforce UNIQUE constraint — prevent duplicate reviews")
        void shouldEnforceUniqueConstraint() {
            createReview(user1, product1, 5, "First", "Text", true);
            entityManager.flush();

            ProductReview duplicate = new ProductReview();
            duplicate.setUser(user1);
            duplicate.setProduct(product1);
            duplicate.setRating(4);
            duplicate.setTitle("Duplicate");
            duplicate.setComment("Text");

            assertThrows(DataIntegrityViolationException.class, () -> {
                reviewRepository.save(duplicate);
                reviewRepository.flush();
            });
        }

        @Test
        @DisplayName("Should allow different users to review same product")
        void shouldAllowDifferentUsersForSameProduct() {
            createReview(user1, product1, 5, "Review 1", "Text", true);
            createReview(user2, product1, 4, "Review 2", "Text", true);
            createReview(user3, product1, 3, "Review 3", "Text", true);

            assertThat(reviewRepository.findApprovedByProductId(product1.getId(), PageRequest.of(0, 10)))
                    .hasSize(3);
        }

        @Test
        @DisplayName("Should allow same user to review different products")
        void shouldAllowSameUserForDifferentProducts() {
            createReview(user1, product1, 5, "Review P1", "Text", true);
            createReview(user1, product2, 4, "Review P2", "Text", true);

            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product1.getId())).isTrue();
            assertThat(reviewRepository.existsByUserIdAndProductId(user1.getId(), product2.getId())).isTrue();
        }
    }

    // ==========================================
    // Helper methods
    // ==========================================

    private User createUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAddress("Test Address");
        return userRepository.save(user);
    }

    private Category createCategory(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    private Product createProduct(String name, Category category, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        return productRepository.save(product);
    }

    private ProductReview createReview(User user, Product product, int rating,
                                       String title, String comment, boolean approved) {
        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setTitle(title);
        review.setComment(comment);
        review.setApproved(approved);
        return reviewRepository.save(review);
    }
}