package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    Order getOrderDetails(Long orderId, User user);
    List<Order> getOrderHistory(User user);
    Order getOrderById(Long id);
    void updateOrderStatus(Long orderId, OrderStatus newStatus);
    Order getOrderForAdmin(Long orderId);
    Page<Order> getAllOrdersForAdmin(Pageable pageable);
    Order checkout(User user);
    Order checkout(User user, String promoCode);
    Order checkout(User user, String promoCode, PaymentProvider paymentProvider);
    Order checkout(User user, String promoCode, PaymentProvider paymentProvider, ShippingMethod shippingMethod);
}