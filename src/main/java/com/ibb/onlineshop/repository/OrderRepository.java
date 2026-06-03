package com.ibb.onlineshop.repository;

import com.ibb.onlineshop.entity.Order;
import com.ibb.onlineshop.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//@Repository
//public interface OrderRepository extends JpaRepository<Order, Long> {
//    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
//    Optional<Order> findByIdAndUserId(Long id, Long userId);
//    long countByStatus(OrderStatus status);
//}