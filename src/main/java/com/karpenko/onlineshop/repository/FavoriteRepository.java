package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @Query("SELECT f FROM Favorite f " +
            "JOIN FETCH f.product p " +
            "LEFT JOIN FETCH p.category " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.createdAt DESC")
    List<Favorite> findAllByUserIdWithProductAndCategory(@Param("userId") Long userId);
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}