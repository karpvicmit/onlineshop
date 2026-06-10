package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto;
import com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto;
import com.karpenko.onlineshop.dto.analytics.TopProductDto;
import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Order, Long> {

    /**
     * Berechnet den Gesamtumsatz pro Monat und Jahr (stornierte Bestellungen werden ausgeschlossen).
     */
    @Query("SELECT new com.karpenko.onlineshop.dto.analytics.MonthlyRevenueDto(" +
            "YEAR(o.orderDate), MONTH(o.orderDate), SUM(o.totalAmount)) " +
            "FROM Order o WHERE o.status != OrderStatus.CANCELLED " +
            "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
            "ORDER BY YEAR(o.orderDate) DESC, MONTH(o.orderDate) DESC")
    List<MonthlyRevenueDto> findMonthlyRevenue();

    /**
     * Ermittelt die Top-10-Produkte nach verkaufter Menge.
     */
    @Query("SELECT new com.karpenko.onlineshop.dto.analytics.TopProductDto(p.name, SUM(oi.quantity)) " +
            "FROM OrderItem oi JOIN oi.product p " +
            "GROUP BY p.id, p.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopProductDto> findTop10Products(Pageable pageable);

    /**
     * Zählt die Anzahl der Bestellungen pro Status.
     */
    @Query("SELECT new com.karpenko.onlineshop.dto.analytics.OrderStatusCountDto(o.status, COUNT(o)) " +
            "FROM Order o GROUP BY o.status")
    List<OrderStatusCountDto> countOrdersByStatus();
}