package com.karpenko.onlineshop.specification;

import com.karpenko.onlineshop.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;


public class ProductSpecification {

    /**
     * Erstellt ein Specification für die Suche nach Produktname (case-insensitive, like)
     * und/oder Kategorie (exakte Übereinstimmung des Slugs).
     *
     * @param name Suchbegriff für den Produktnamen (kann null oder leer sein)
     * @param categorySlug Slug der Kategorie (kann null oder leer sein)
     * @return Specification<Product>
     */
    public static Specification<Product> hasNameAndCategory(String name, String categorySlug) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (name != null && !name.trim().isEmpty()) {
                String likePattern = "%" + name.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), likePattern));
            }

            if (categorySlug != null && !categorySlug.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("slug"), categorySlug.trim().toLowerCase()));
            }

            return predicate;
        };
    }
}