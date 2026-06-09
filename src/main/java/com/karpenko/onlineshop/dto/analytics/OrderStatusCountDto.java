package com.karpenko.onlineshop.dto.analytics;

import com.karpenko.onlineshop.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderStatusCountDto {
    private OrderStatus status;
    private Long count;
}