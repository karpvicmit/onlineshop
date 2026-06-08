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
        log.info("Toggle Favorite: User {}, Product {}", user.getId(), productId);

        favoriteRepository.findByUserIdAndProductId(user.getId(), productId).ifPresentOrElse(
                favorite -> {
                    favoriteRepository.delete(favorite);
                    log.info("Produkt {} aus Favoriten von {} entfernt", productId, user.getEmail());
                },
                () -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Produkt mit ID " + productId + " nicht gefunden"));

                    Favorite favorite = new Favorite();
                    favorite.setUser(user);
                    favorite.setProduct(product);
                    favoriteRepository.save(favorite);
                    log.info("Produkt {} zu Favoriten von {} hinzugefügt", productId, user.getEmail());
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesForUser(User user) {
        return favoriteRepository.findByUserIdOrderByProduct_NameAsc(user.getId());
    }
}