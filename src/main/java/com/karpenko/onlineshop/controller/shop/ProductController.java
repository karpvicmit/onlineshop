package com.karpenko.onlineshop.controller.shop;


import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Controller
@RequestMapping("/shop/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        log.info("Anfrage Produktliste: q='{}', category='{}', page={}", q, category, page);

        Pageable pageable = PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "id"));

        Page<ProductDto> productPage = productService.findProducts(q, category, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "shop/products/list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        log.info("Anfrage Produktdetail für ID: {}", id);

        ProductDto product = productService.getProductById(id);
        model.addAttribute("product", product);

        return "shop/products/detail";
    }
}