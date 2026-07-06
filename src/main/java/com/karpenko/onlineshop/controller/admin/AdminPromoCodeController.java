package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.dto.promo.PromoCodeDto;
import com.karpenko.onlineshop.entity.DiscountType;
import com.karpenko.onlineshop.entity.PromoCode;
import com.karpenko.onlineshop.service.PromoCodeAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/promocodes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPromoCodeController {

    private final PromoCodeAdminService promoCodeAdminService;

    @ModelAttribute("activeMenu")
    public String activeMenu() {
        return "promocodes";
    }

    @ModelAttribute("discountTypes")
    public List<DiscountType> discountTypes() {
        return List.of(DiscountType.values());
    }

    @GetMapping
    public String listPromoCodes(Model model) {
        log.debug("Admin viewing all promo codes");
        List<PromoCodeDto> promoCodes = promoCodeAdminService.getAllPromoCodes();
        model.addAttribute("promoCodes", promoCodes);
        return "admin/promocodes/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        PromoCode promoCode = new PromoCode();
        promoCode.setDiscountType(DiscountType.PERCENTAGE);
        promoCode.setValidFrom(LocalDateTime.now());
        promoCode.setActive(true);
        model.addAttribute("promoCode", promoCode);
        return "admin/promocodes/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        PromoCodeDto dto = promoCodeAdminService.getPromoCodeById(id);
        PromoCode promoCode = mapDtoToEntity(dto);
        model.addAttribute("promoCode", promoCode);
        return "admin/promocodes/form";
    }

    @PostMapping("/save")
    public String savePromoCode(@Valid @ModelAttribute("promoCode") PromoCode promoCode,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/promocodes/form";
        }

        try {
            promoCodeAdminService.savePromoCode(promoCode);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Promo-Code erfolgreich gespeichert.");
            log.info("Promo code saved: {}", promoCode.getCode());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate promo code: {}", promoCode.getCode());
            bindingResult.rejectValue("code", "duplicate",
                    "Dieser Promo-Code existiert bereits.");
            return "admin/promocodes/form";
        } catch (Exception e) {
            log.error("Error saving promo code", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Fehler beim Speichern: " + e.getMessage());
        }

        return "redirect:/admin/promocodes";
    }

    @PostMapping("/delete/{id}")
    public String deletePromoCode(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            promoCodeAdminService.deletePromoCode(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Promo-Code erfolgreich gelöscht.");
            log.info("Promo code deleted: ID {}", id);
        } catch (Exception e) {
            log.error("Error deleting promo code", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Fehler beim Löschen: " + e.getMessage());
        }
        return "redirect:/admin/promocodes";
    }

    @PostMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            promoCodeAdminService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Status des Promo-Codes erfolgreich geändert.");
            log.info("Promo code {} toggled", id);
        } catch (Exception e) {
            log.error("Error toggling promo code", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Fehler: " + e.getMessage());
        }
        return "redirect:/admin/promocodes";
    }

    private PromoCode mapDtoToEntity(PromoCodeDto dto) {
        PromoCode promoCode = new PromoCode();
        promoCode.setId(dto.getId());
        promoCode.setCode(dto.getCode());
        promoCode.setDiscountType(dto.getDiscountType());
        promoCode.setDiscountValue(dto.getDiscountValue());
        promoCode.setMinOrderAmount(dto.getMinOrderAmount());
        promoCode.setMaxUses(dto.getMaxUses());
        promoCode.setUsedCount(dto.getUsedCount());
        promoCode.setValidFrom(dto.getValidFrom());
        promoCode.setValidUntil(dto.getValidUntil());
        promoCode.setActive(dto.getActive());
        return promoCode;
    }
}