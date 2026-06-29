package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.dto.review.ReviewStatsDto;
import com.karpenko.onlineshop.entity.ProductReview;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.service.ProductReviewService;
import com.karpenko.onlineshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/shop/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductReviewService productReviewService;

    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        log.info("Anfrage Produktliste: q='{}', category='{}', sort='{}', page={}",
                q, category, sort, page);

        // Определяем сортировку
        Sort sortBy = getSortBy(sort);
        Pageable pageable = PageRequest.of(page, 12, sortBy);

        Page<ProductDto> productPage = productService.findProducts(q, category, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "shop/products/list";
    }

    private Sort getSortBy(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "date_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // По умолчанию: сначала новые
        };
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model,
                                @org.springframework.security.core.annotation.AuthenticationPrincipal
                                com.karpenko.onlineshop.security.CustomUserDetails userDetails) {

        log.info("Anfrage Produktdetail für ID: {}", id);
        ProductDto product = productService.getProductById(id);
        model.addAttribute("product", product);

        // Reviews logic
        Page<ProductReview> reviewsPage = productReviewService.getApprovedReviews(
                id, org.springframework.data.domain.PageRequest.of(0, 5));
        ReviewStatsDto stats = productReviewService.getReviewStats(id);

        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("reviewStats", stats);

        boolean canWriteReview = false;
        if (userDetails != null) {
            canWriteReview = productReviewService.hasUserPurchasedAndNotReviewed(
                    userDetails.getId(), id);
        }
        model.addAttribute("canWriteReview", canWriteReview);

        return "shop/products/detail";
    }

    @PostMapping("/{id}/reviews")
    public String submitReview(@PathVariable Long id,
                               @RequestParam Integer rating,
                               @RequestParam String title,
                               @RequestParam String comment,
                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                               com.karpenko.onlineshop.security.CustomUserDetails userDetails,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            productReviewService.addReview(userDetails.getId(), id, rating, title, comment);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Vielen Dank! Ihre Bewertung wird nach einer Überprüfung angezeigt.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/shop/products/" + id;
    }
}