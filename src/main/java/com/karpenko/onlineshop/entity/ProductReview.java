package com.karpenko.onlineshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Min(value = 1, message = "{validation.review.rating.min}")
    @Max(value = 5, message = "{validation.review.rating.max}")
    @Column(nullable = false)
    private Integer rating;

    @NotBlank(message = "{validation.review.title.required}")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "{validation.review.comment.required}")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(nullable = false)
    private boolean approved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductReview that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}