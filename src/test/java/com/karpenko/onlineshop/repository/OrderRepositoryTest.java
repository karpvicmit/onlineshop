package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderRepository - JPQL Queries")
class OrderRepositoryTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("test@test.de");
        user.setPasswordHash("hash");
        user.setAddress("Test Address");
        user = userRepository.save(user);

        Category category = new Category();
        category.setName("Test");
        category.setSlug("test");
        category = categoryRepository.save(category);

        product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(100);
        product.setCategory(category);
        product.setSku("ORDER-TEST-SKU");
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("Should find orders by user ID with items eagerly loaded")
    void shouldFindOrdersByUserIdWithItems() {
        Order order = createOrder(user, product, 2, new BigDecimal("200.00"));
        orderRepository.save(order);

        List<Order> orders = orderRepository.findByUserIdWithItems(user.getId());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderItems()).hasSize(1);
        assertThat(orders.get(0).getOrderItems().get(0).getProduct().getName())
                .isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should support pagination for admin orders")
    void shouldSupportPagination() {
        for (int i = 0; i < 5; i++) {
            orderRepository.save(createOrder(user, product, 1, new BigDecimal("100.00")));
        }

        Page<Order> page0 = orderRepository.findAllWithUserAndItems(PageRequest.of(0, 2));
        Page<Order> page1 = orderRepository.findAllWithUserAndItems(PageRequest.of(1, 2));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
    }

    private Order createOrder(User user, Product product, int quantity, BigDecimal total) {
        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(user.getAddress());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.NEW);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        order.addItem(item);

        return order;
    }
}