package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.util.MessageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final MessageUtil messageUtil;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "categories"; }

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories/form";
    }

    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("category") Category category,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/categories/form";
        }

        try {
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.categories.save.success"));
        } catch (Exception e) {
            log.error("Fehler beim Speichern der Kategorie: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.categories.save.error", e.getMessage()));
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Kategorie mit ID " + id + " nicht gefunden"));
        model.addAttribute("category", category);
        return "admin/categories/form";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.categories.delete.success"));
        } catch (Exception e) {
            log.error("Fehler beim Löschen der Kategorie: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.categories.delete.error", e.getMessage()));
        }
        return "redirect:/admin/categories";
    }
}