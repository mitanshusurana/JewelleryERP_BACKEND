package com.gemerp.inventory.dto;

import com.gemerp.inventory.model.ProductType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateProductRequest(
    @NotEmpty(message = "QR Code ID cannot be empty") String qrCodeId,
    @NotNull(message = "Product type must be specified") ProductType productType,
    Map<String, String> attributes
) {}