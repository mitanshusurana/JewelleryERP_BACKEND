package com.gemerp.inventory.dto;

import com.gemerp.inventory.model.ProductType;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String qrCodeId,
    ProductType productType,
    String name,
    Map<String, String> attributes
) {}
