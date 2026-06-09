package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.entity.User;

import java.util.List;

public interface OrderService {
    Order checkout(User user);
    Order getOrderDetails(Long orderId, User user);
    List<Order> getOrderHistory(User user);

    void updateOrderStatus(Long orderId, OrderStatus newStatus);
}