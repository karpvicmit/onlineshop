package com.karpenko.onlineshop.specification;

import com.karpenko.onlineshop.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasNameAndCategory(String name, String categorySlug) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (name != null && !name.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            if (categorySlug != null && !categorySlug.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("slug"), categorySlug.trim().toLowerCase()));
            }
            return predicate;
        };
    }

    public static Specification<Product> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }
}