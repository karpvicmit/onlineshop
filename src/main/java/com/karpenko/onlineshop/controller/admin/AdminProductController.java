package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.service.ProductService;
import com.karpenko.onlineshop.util.MessageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final MessageUtil messageUtil;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "products"; }

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductDto> productPage = productService.findProducts(null, null, pageable);
        model.addAttribute("productPage", productPage);
        model.addAttribute("baseUrl", "/admin/products");
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/products/form";
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product,
                              BindingResult bindingResult,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/products/form";
        }

        try {
            productService.saveProduct(product, imageFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.products.save.success"));
        } catch (Exception e) {
            log.error("Fehler beim Speichern des Produkts: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.products.save.error", e.getMessage()));
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductEntityById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/products/form";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.products.delete.success"));
        } catch (Exception e) {
            log.error("Fehler beim Löschen des Produkts: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.products.delete.error", e.getMessage()));
        }
        return "redirect:/admin/products";
    }
}