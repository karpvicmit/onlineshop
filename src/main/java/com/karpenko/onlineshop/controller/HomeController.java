package com.karpenko.onlineshop.controller;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        log.debug("Serving home page");

        // Featured products: latest 8 products
        List<ProductDto> featuredProducts = productService
                .findProducts(null, null, PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();

        // Categories: first 6
        List<Category> categories = categoryService.getAllCategories();
        if (categories.size() > 6) {
            categories = categories.subList(0, 6);
        }

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("categories", categories);

        return "home";
    }
}