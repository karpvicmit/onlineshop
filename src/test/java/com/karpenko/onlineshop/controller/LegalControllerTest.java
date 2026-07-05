package com.karpenko.onlineshop.controller;

import com.karpenko.onlineshop.config.BaseWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LegalController.class)
@DisplayName("LegalController - Public Legal Pages")
class LegalControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /impressum should return 200 and legal/impressum view")
    void shouldShowImpressumPage() throws Exception {
        mockMvc.perform(get("/impressum"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/impressum"));
    }

    @Test
    @DisplayName("GET /agb should return 200 and legal/agb view")
    void shouldShowAgbPage() throws Exception {
        mockMvc.perform(get("/agb"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/agb"));
    }

    @Test
    @DisplayName("GET /datenschutz should return 200 and legal/datenschutz view")
    void shouldShowDatenschutzPage() throws Exception {
        mockMvc.perform(get("/datenschutz"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/datenschutz"));
    }

    @Test
    @DisplayName("GET /widerruf should return 200 and legal/widerruf view")
    void shouldShowWiderrufPage() throws Exception {
        mockMvc.perform(get("/widerruf"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/widerruf"));
    }

    @Test
    @DisplayName("Legal pages should be accessible WITHOUT authentication")
    void shouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/impressum")).andExpect(status().isOk());
        mockMvc.perform(get("/agb")).andExpect(status().isOk());
        mockMvc.perform(get("/datenschutz")).andExpect(status().isOk());
        mockMvc.perform(get("/widerruf")).andExpect(status().isOk());
    }
}