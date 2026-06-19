package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Favorite;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.FavoriteRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService - Toggle Logic /T10/")
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");

        product = new Product();
        product.setId(10L);
        product.setName("iPhone");
    }

    @Test
    @DisplayName("/T10/ Should ADD favorite when not yet present")
    void shouldAddFavorite() {
        when(favoriteRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

        favoriteService.toggleFavorite(user, 10L);

        verify(favoriteRepository).save(argThat(fav ->
                fav.getUser().equals(user) && fav.getProduct().equals(product)));
    }

    @Test
    @DisplayName("/T10/ Should REMOVE favorite when already present (toggle)")
    void shouldRemoveFavorite() {
        Favorite existing = new Favorite();
        existing.setUser(user);
        existing.setProduct(product);

        when(favoriteRepository.findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(existing));

        favoriteService.toggleFavorite(user, 10L);

        verify(favoriteRepository).delete(existing);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle race condition gracefully (UNIQUE constraint)")
    void shouldHandleDuplicateRaceCondition() {
        when(favoriteRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteRepository.save(any(Favorite.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        // Should NOT throw - race condition is logged and ignored
        favoriteService.toggleFavorite(user, 10L);
    }

    @Test
    @DisplayName("Should throw when product does not exist")
    void shouldThrowWhenProductMissing() {
        when(favoriteRepository.findByUserIdAndProductId(1L, 99L)).thenReturn(Optional.empty());
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.toggleFavorite(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}