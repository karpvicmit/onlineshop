package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.User;

public interface OrderService {
    Order checkout(User user);
    Order getOrderDetails(Long orderId, User user);
    java.util.List<Order> getOrderHistory(User user);
}