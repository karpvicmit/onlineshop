package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.entity.PaymentStatus;
import com.karpenko.onlineshop.service.EmailService;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.util.MessageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final MessageUtil messageUtil;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "orders"; }

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));
        Page<Order> orderPage = orderService.getAllOrdersForAdmin(pageable);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
        return "admin/orders/list";
    }

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderForAdmin(id));
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.orders.status.update.success"));
        } catch (Exception e) {
            log.warn("Failed to update order status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.orders.status.update.error", e.getMessage()));
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/mark-as-paid")
    public String markAsPaid(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findByIdWithUserAndItems(id)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Order not found"));

            if (order.getPaymentStatus() == PaymentStatus.SUCCEEDED) {
                redirectAttributes.addFlashAttribute("infoMessage",
                        messageUtil.get("admin.orders.markAsPaid.already", id));
                return "redirect:/admin/orders";
            }

            order.setPaymentStatus(PaymentStatus.SUCCEEDED);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);

            // Notify customer about successful payment
            try {
                emailService.sendPaymentSuccessEmail(order.getUser().getEmail(), order);
            } catch (Exception e) {
                log.warn("Failed to send payment success email for order {}: {}",
                        id, e.getMessage());
            }

            log.info("Order #{} marked as paid by admin", id);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.orders.markAsPaid.success", id));
        } catch (Exception e) {
            log.error("Failed to mark order #{} as paid", id, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.orders.markAsPaid.error", e.getMessage()));
        }
        return "redirect:/admin/orders";
    }
}