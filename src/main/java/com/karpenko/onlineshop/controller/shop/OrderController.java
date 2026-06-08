package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/shop/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    public String getOrderHistory(Model model) {
        User user = userService.getCurrentUser();
        List<Order> orders = orderService.getOrderHistory(user);
        model.addAttribute("orders", orders);
        return "shop/orders/history";
    }

    @GetMapping("/{id}")
    public String getOrderDetail(@PathVariable Long id, Model model) {
        User user = userService.getCurrentUser();
        Order order = orderService.getOrderDetails(id, user);
        model.addAttribute("order", order);
        return "shop/orders/detail";
    }
}