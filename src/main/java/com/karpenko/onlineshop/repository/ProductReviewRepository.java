package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    @Query("SELECT r FROM ProductReview r JOIN FETCH r.user " +
            "WHERE r.product.id = :productId AND r.approved = true " +
            "ORDER BY r.createdAt DESC")
    Page<ProductReview> findApprovedByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT new com.karpenko.onlineshop.dto.review.ReviewStatsDto(" +
            "COALESCE(AVG(r.rating), 0.0), COUNT(r)) " +
            "FROM ProductReview r " +
            "WHERE r.product.id = :productId AND r.approved = true")
    Optional<ReviewStatsDto> findReviewStatsByProductId(@Param("productId") Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT r FROM ProductReview r JOIN FETCH r.user JOIN FETCH r.product " +
            "WHERE r.approved = false ORDER BY r.createdAt ASC")
    List<ProductReview> findAllPending();
}