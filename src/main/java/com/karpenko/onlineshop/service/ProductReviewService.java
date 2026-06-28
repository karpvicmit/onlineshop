package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductReviewService {
    void addReview(Long userId, Long productId, Integer rating, String title, String comment);
    Page<ProductReview> getApprovedReviews(Long productId, Pageable pageable);
    ReviewStatsDto getReviewStats(Long productId);
    boolean hasUserPurchasedAndNotReviewed(Long userId, Long productId);

    // Admin methods
    List<ProductReview> getPendingReviews();
    void approveReview(Long reviewId);
    void rejectReview(Long reviewId);
}