package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto;
import com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto;
import com.karpenko.onlineshop.dto.analytics.TopProductDto;
import com.karpenko.onlineshop.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AnalyticsRepository extends Repository<Order, Long> {

    @Query("SELECT new com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto(" +
            "YEAR(o.orderDate), MONTH(o.orderDate), SUM(o.totalAmount)) " +
            "FROM Order o WHERE o.status != com.karpenko.onlineshop.entity.OrderStatus.CANCELLED " +
            "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
            "ORDER BY YEAR(o.orderDate) ASC, MONTH(o.orderDate) ASC")
    List<MonthlyRevenueDto> findMonthlyRevenue();

    @Query("SELECT new com.karpenko.onlineshop.dto.analytics.TopProductDto(p.name, SUM(oi.quantity)) " +
            "FROM OrderItem oi JOIN oi.product p " +
            "GROUP BY p.id, p.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopProductDto> findTop10Products(Pageable pageable);

    @Query("SELECT new com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto(o.status, COUNT(o)) " +
            "FROM Order o GROUP BY o.status")
    List<OrderStatusCountDto> countOrdersByStatus();
}