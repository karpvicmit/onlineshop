package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto;
import com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto;
import com.karpenko.onlineshop.dto.analytics.TopProductDto;
import com.karpenko.onlineshop.repository.AnalyticsRepository;
import com.karpenko.onlineshop.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyRevenueDto> getMonthlyRevenue() {
        log.debug("Lade monatliche Umsatzdaten");
        return analyticsRepository.findMonthlyRevenue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductDto> getTop10Products() {
        log.debug("Lade Top-10-Produkte");
        return analyticsRepository.findTop10Products(PageRequest.of(0, 10));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusCountDto> getOrdersByStatus() {
        log.debug("Lade Bestellanzahl pro Status");
        return analyticsRepository.countOrdersByStatus();
    }
}