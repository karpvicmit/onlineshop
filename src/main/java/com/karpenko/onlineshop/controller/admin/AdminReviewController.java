package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.entity.ProductReview;
import com.karpenko.onlineshop.service.ProductReviewService;
import com.karpenko.onlineshop.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ProductReviewService reviewService;
    private final MessageUtil messageUtil;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "reviews"; }

    @GetMapping
    public String listPendingReviews(Model model) {
        List<ProductReview> pendingReviews = reviewService.getPendingReviews();
        model.addAttribute("reviews", pendingReviews);
        return "admin/reviews/list";
    }

    @PostMapping("/{id}/approve")
    public String approveReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.approveReview(id);
        redirectAttributes.addFlashAttribute("successMessage",
                messageUtil.get("admin.reviews.approve.success"));
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/reject")
    public String rejectReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.rejectReview(id);
        redirectAttributes.addFlashAttribute("successMessage",
                messageUtil.get("admin.reviews.reject.success"));
        return "redirect:/admin/reviews";
    }
}