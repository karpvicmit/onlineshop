package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.entity.Favorite;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.FavoriteRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void toggleFavorite(User user, Long productId) {
        log.info("Toggling favorite: user {}, product {}", user.getId(), productId);

        favoriteRepository.findByUserIdAndProductId(user.getId(), productId).ifPresentOrElse(
                favorite -> {
                    favoriteRepository.delete(favorite);
                    log.info("Product {} removed from favorites for user {}", productId, user.getEmail());
                },
                () -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                    Favorite favorite = new Favorite();
                    favorite.setUser(user);
                    favorite.setProduct(product);
                    try {
                        favoriteRepository.save(favorite);
                        log.info("Product {} added to favorites for user {}", productId, user.getEmail());
                    } catch (DataIntegrityViolationException ex) {
                        log.warn("Duplicate favorite attempt (race condition), ignoring.");
                    }
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByUserId(Long userId) {
        log.info("Loading favorites for user ID: {}", userId);
        return favoriteRepository.findAllByUserIdWithProductAndCategory(userId);
    }
}