package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Favorite;
import com.karpenko.onlineshop.entity.User;
import java.util.List;

public interface FavoriteService {
    void toggleFavorite(User user, Long productId);
    List<Favorite> getFavoritesForUser(User user);
}