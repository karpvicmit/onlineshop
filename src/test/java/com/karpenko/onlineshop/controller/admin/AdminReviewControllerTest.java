package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.config.BaseWebMvcTest;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.entity.ProductReview;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.ProductReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReviewController.class)
@DisplayName("AdminReviewController - Review Moderation")
class AdminReviewControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductReviewService reviewService;

    @Test
    @DisplayName("Admin should see list of pending reviews")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldListPendingReviews() throws Exception {
        ProductReview review = buildReview(1L, "Great product", 5);
        when(reviewService.getPendingReviews()).thenReturn(List.of(review));

        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reviews/list"))
                .andExpect(model().attributeExists("reviews"));

        verify(reviewService).getPendingReviews();
    }

    @Test
    @DisplayName("Admin should approve review and redirect with success message")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldApproveReview() throws Exception {
        doNothing().when(reviewService).approveReview(100L);

        mockMvc.perform(post("/admin/reviews/100/approve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reviews"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(reviewService).approveReview(100L);
    }

    @Test
    @DisplayName("Admin should reject review and redirect with success message")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldRejectReview() throws Exception {
        doNothing().when(reviewService).rejectReview(100L);

        mockMvc.perform(post("/admin/reviews/100/reject").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reviews"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(reviewService).rejectReview(100L);
    }


    @Test
    @DisplayName("USER should get 403 on /admin/reviews")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldDenyUserAccess() throws Exception {
        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should be redirected to /login")
    void shouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/admin/reviews"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private ProductReview buildReview(Long id, String title, int rating) {
        ProductReview review = new ProductReview();
        review.setId(id);
        review.setTitle(title);
        review.setComment("Comment text");
        review.setRating(rating);
        review.setApproved(false);
        review.setCreatedAt(LocalDateTime.now());

        User user = new User();
        user.setEmail("user@test.de");
        user.setFirstName("Max");
        user.setLastName("M.");
        review.setUser(user);

        Product product = new Product();
        product.setName("Test Product");
        review.setProduct(product);

        return review;
    }
}