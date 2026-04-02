package com.example.project.admin.dto;

import java.time.LocalDateTime;

public record AdminOfferResponse(
    Long offerId,
    String bookTitle,
    String ownerName,
    String condition,
    String status,
    LocalDateTime createdAt
) {
}
