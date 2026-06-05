package com.karpenko.onlineshop.controller.shop;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    @GetMapping("/shop/products")
    public String productList() {
        return "Product List!";
    }
}