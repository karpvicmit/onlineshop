package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    Order checkout(User user);
    Order getOrderDetails(Long orderId, User user);
    List<Order> getOrderHistory(User user);
    Order getOrderById(Long id);
    void updateOrderStatus(Long orderId, OrderStatus newStatus);
    Order getOrderForAdmin(Long orderId);
    Page<Order> getAllOrdersForAdmin(Pageable pageable);
    Order checkout(User user, String promoCode);
}