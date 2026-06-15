package com.karpenko.onlineshop.mapper;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ProductMapper {

    @Mapping(source = "category.name", target = "categoryName")
    ProductDto toDto(Product product);
}
