package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.entity.ProductReview;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.OrderItemRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.repository.ProductReviewRepository;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public void addReview(Long userId, Long productId, Integer rating, String title, String comment) {
        if (!hasUserPurchasedAndNotReviewed(userId, productId)) {
            throw new IllegalStateException("You can only review products you have purchased and haven't reviewed yet.");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findActiveById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setTitle(title);
        review.setComment(comment);

        reviewRepository.save(review);
        log.info("New review submitted for product {} by user {}", productId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReview> getApprovedReviews(Long productId, Pageable pageable) {
        return reviewRepository.findApprovedByProductId(productId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsDto getReviewStats(Long productId) {
        return reviewRepository.findReviewStatsByProductId(productId)
                .orElse(new ReviewStatsDto(0.0, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPurchasedAndNotReviewed(Long userId, Long productId) {
        boolean purchased = orderItemRepository.hasUserPurchasedProduct(userId, productId);
        boolean alreadyReviewed = reviewRepository.existsByUserIdAndProductId(userId, productId);
        return purchased && !alreadyReviewed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> getPendingReviews() {
        return reviewRepository.findAllPending();
    }

    @Override
    @Transactional
    public void approveReview(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setApproved(true);
        reviewRepository.save(review);
        log.info("Review {} approved", reviewId);
    }

    @Override
    @Transactional
    public void rejectReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
        log.info("Review {} rejected and deleted", reviewId);
    }
}