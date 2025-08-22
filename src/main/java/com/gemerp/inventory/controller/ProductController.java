package com.gemerp.inventory.controller;

import com.gemerp.inventory.dto.CreateProductRequest;
import com.gemerp.inventory.dto.ProductResponse;
import com.gemerp.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse productResponse = productService.createProduct(request);
        return new ResponseEntity<>(productResponse, HttpStatus.CREATED);
    }
}
