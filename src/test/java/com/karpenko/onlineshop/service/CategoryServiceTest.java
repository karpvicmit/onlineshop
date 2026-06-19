package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Slug Generation")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("Should auto-generate URL-friendly slug from German name")
    void shouldGenerateSlugFromGermanName() {
        Category category = new Category();
        category.setName("Sport & Freizeit");
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.saveCategory(category);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("sport-freizeit");
    }

    @Test
    @DisplayName("Should handle umlauts in slug generation")
    void shouldHandleUmlauts() {
        Category category = new Category();
        category.setName("Bücher & Möbel");
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.saveCategory(category);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("buecher-moebel");
    }
}