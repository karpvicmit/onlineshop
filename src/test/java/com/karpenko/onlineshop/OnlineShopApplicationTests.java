package com.karpenko.onlineshop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context - Smoke Test")
class OnlineShopApplicationTests {

    @Test
    @DisplayName("Spring application context should load successfully")
    void contextLoads() {
    }
}