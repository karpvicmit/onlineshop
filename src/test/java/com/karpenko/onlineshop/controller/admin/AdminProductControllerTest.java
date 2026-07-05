package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.config.BaseWebMvcTest;
import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.service.CategoryService;
import com.karpenko.onlineshop.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProductController.class)
@DisplayName("AdminProductController - Product CRUD")
class AdminProductControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("Admin should create new product successfully")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldCreateProductSuccessfully() throws Exception {
        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 15");
        product.setPrice(new BigDecimal("999.00"));
        product.setStock(10);
        product.setCategory(new Category());
        product.setSku("SKU-001");

        when(productService.saveProduct(any(Product.class), any(MultipartFile.class)))
                .thenReturn(product);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "iphone.jpg", "image/jpeg", "fake-image".getBytes());

        mockMvc.perform(multipart("/admin/products/save")
                        .file(imageFile)
                        .param("name", "iPhone 15")
                        .param("description", "New iPhone")
                        .param("price", "999.00")
                        .param("stock", "10")
                        .param("category.id", "1")
                        .param("sku", "SKU-001")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        verify(productService).saveProduct(any(Product.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("Admin should see product list")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldListProducts() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setId(1L);
        dto.setName("iPhone");
        dto.setPrice(new BigDecimal("999.00"));
        dto.setStock(10);

        Page<ProductDto> page = new PageImpl<>(List.of(dto));
        when(productService.findProducts(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/list"))
                .andExpect(model().attributeExists("productPage"));
    }

    @Test
    @DisplayName("Admin should see create form")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldShowCreateForm() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Elektronik");
        when(categoryService.getAllCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/admin/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/form"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("Admin should soft-delete product")
    @WithMockUser(username = "admin@test.de", roles = {"ADMIN"})
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(10L);

        mockMvc.perform(post("/admin/products/delete/10").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        verify(productService).deleteProduct(10L);
    }

    @Test
    @DisplayName("USER should get 403 on /admin/products")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldDenyUserAccess() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should be redirected to /login")
    void shouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}