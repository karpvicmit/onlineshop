package com.karpenko.onlineshop.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Slf4j
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        log.info("Administrator hat das Dashboard aufgerufen.");
        return "admin/dashboard";
    }
}