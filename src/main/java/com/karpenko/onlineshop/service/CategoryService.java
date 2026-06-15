package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {

    List<Category> getAllCategories();

    Optional<Category> getCategoryById(Long id);

    Optional<Category> getCategoryBySlug(String slug);

    Category saveCategory(Category category);

    void deleteCategory(Long id);
}