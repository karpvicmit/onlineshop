package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        log.debug("Alle Kategorien werden aus der Datenbank abgerufen.");
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        log.debug("Kategorie mit ID {} wird gesucht.", id);
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryBySlug(String slug) {
        log.debug("Kategorie mit Slug '{}' wird gesucht.", slug);
        return categoryRepository.findBySlug(slug);
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        log.info("Kategorie wird gespeichert oder aktualisiert: {}", category.getName());

        String generatedSlug = SlugUtil.generateSlug(category.getName());
        category.setSlug(generatedSlug);

        log.debug("Generierter Slug für '{}': {}", category.getName(), generatedSlug);

        Category savedCategory = categoryRepository.save(category);
        log.info("Kategorie erfolgreich mit ID {} gespeichert.", savedCategory.getId());
        return savedCategory;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Versuch, Kategorie mit ID {} zu löschen.", id);

        if (!categoryRepository.existsById(id)) {
            log.warn("Löschversuch fehlgeschlagen: Kategorie mit ID {} existiert nicht.", id);
            throw new IllegalArgumentException("Kategorie mit ID " + id + " nicht gefunden.");
        }

        categoryRepository.deleteById(id);
        log.info("Kategorie mit ID {} erfolgreich gelöscht.", id);
    }
}