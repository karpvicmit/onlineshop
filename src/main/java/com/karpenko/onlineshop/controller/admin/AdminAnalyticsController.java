package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto;
import com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto;
import com.karpenko.onlineshop.dto.analytics.TopProductDto;
import com.karpenko.onlineshop.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Slf4j
@Controller
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "analytics"; }

    @GetMapping
    public String showDashboard(Model model) {
        log.info("Admin ruft Analytics-Dashboard auf");

        List<MonthlyRevenueDto> revenueData = analyticsService.getMonthlyRevenue();
        List<TopProductDto> topProducts = analyticsService.getTop10Products();
        List<OrderStatusCountDto> statusCounts = analyticsService.getOrdersByStatus();

        model.addAttribute("revenueData", revenueData);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("statusCounts", statusCounts);
        return "admin/analytics";
    }
}