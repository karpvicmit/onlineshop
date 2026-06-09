package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto;
import com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto;
import com.karpenko.onlineshop.dto.analytics.TopProductDto;
import java.util.List;

public interface AnalyticsService {
    List<MonthlyRevenueDto> getMonthlyRevenue();
    List<TopProductDto> getTop10Products();
    List<OrderStatusCountDto> getOrdersByStatus();
}