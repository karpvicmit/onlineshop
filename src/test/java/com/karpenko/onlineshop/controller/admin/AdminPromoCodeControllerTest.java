package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.config.BaseWebMvcTest;
import com.karpenko.onlineshop.dto.promo.PromoCodeDto;
import com.karpenko.onlineshop.entity.DiscountType;
import com.karpenko.onlineshop.entity.PromoCode;
import com.karpenko.onlineshop.service.PromoCodeAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPromoCodeController.class)
@DisplayName("AdminPromoCodeController - Promo Code CRUD")
class AdminPromoCodeControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromoCodeAdminService promoCodeAdminService;

    @Test
    @DisplayName("Admin should see list of promo codes")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldListPromoCodes() throws Exception {
        PromoCodeDto dto = PromoCodeDto.builder()
                .id(1L)
                .code("SAVE10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usedCount(0)
                .maxUses(100)
                .validFrom(LocalDateTime.now())
                .active(true)
                .currentlyActive(true)
                .build();

        when(promoCodeAdminService.getAllPromoCodes()).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/promocodes"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/promocodes/list"))
                .andExpect(model().attributeExists("promoCodes"));

        verify(promoCodeAdminService).getAllPromoCodes();
    }

    @Test
    @DisplayName("Admin should see create form")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldShowCreateForm() throws Exception {
        mockMvc.perform(get("/admin/promocodes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/promocodes/form"))
                .andExpect(model().attributeExists("promoCode"))
                .andExpect(model().attributeExists("discountTypes"));
    }

    @Test
    @DisplayName("Admin should create promo code successfully")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldCreatePromoCode() throws Exception {
        when(promoCodeAdminService.savePromoCode(any(PromoCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/admin/promocodes/save")
                        .with(csrf())
                        .param("code", "SAVE10")
                        .param("discountType", "PERCENTAGE")
                        .param("discountValue", "10.00")
                        .param("validFrom", "2026-01-01T00:00")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promocodes"));

        verify(promoCodeAdminService).savePromoCode(any(PromoCode.class));
    }

    @Test
    @DisplayName("Admin should toggle promo code active status")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldToggleActive() throws Exception {
        doNothing().when(promoCodeAdminService).toggleActive(1L);

        mockMvc.perform(post("/admin/promocodes/toggle/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promocodes"));

        verify(promoCodeAdminService).toggleActive(1L);
    }

    @Test
    @DisplayName("Admin should delete promo code")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldDeletePromoCode() throws Exception {
        doNothing().when(promoCodeAdminService).deletePromoCode(1L);

        mockMvc.perform(post("/admin/promocodes/delete/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promocodes"));

        verify(promoCodeAdminService).deletePromoCode(1L);
    }

    @Test
    @DisplayName("USER should get 403 on /admin/promocodes")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldDenyUserAccess() throws Exception {
        mockMvc.perform(get("/admin/promocodes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should be redirected to /login")
    void shouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/admin/promocodes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}