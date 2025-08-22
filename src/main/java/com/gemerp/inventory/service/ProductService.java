package com.gemerp.inventory.service;

import com.gemerp.inventory.dto.CreateProductRequest;
import com.gemerp.inventory.dto.ProductResponse;
import com.gemerp.inventory.exception.DuplicateResourceException;
import com.gemerp.inventory.model.Product;
import com.gemerp.inventory.model.ProductAttribute;
import com.gemerp.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        productRepository.findByQrCodeId(request.qrCodeId()).ifPresent(p -> {
            throw new DuplicateResourceException("Product with QR Code ID " + request.qrCodeId() + " already exists.");
        });

        Product product = new Product(request.qrCodeId(), request.productType());
        
        // TODO: Integrate with a real GenAI service
        product.setName("AI Generated Name for " + request.qrCodeId());
        product.setDescription("AI Generated Description...");

        if (request.attributes() != null) {
            request.attributes().forEach((key, value) -> {
                ProductAttribute attribute = new ProductAttribute(key, value);
                product.addAttribute(attribute);
            });
        }

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    private ProductResponse mapToProductResponse(Product product) {
        Map<String, String> attributeMap = product.getAttributes().stream()
                .collect(Collectors.toMap(ProductAttribute::getAttributeName, ProductAttribute::getAttributeValue));

        return new ProductResponse(
                product.getId(),
                product.getQrCodeId(),
                product.getProductType(),
                product.getName(),
                attributeMap
        );
    }
}
