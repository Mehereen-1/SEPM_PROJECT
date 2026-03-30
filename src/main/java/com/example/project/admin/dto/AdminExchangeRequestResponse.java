package com.example.project.admin.dto;

import java.time.LocalDateTime;

public record AdminExchangeRequestResponse(
    Long exchangeRequestId,
    String requesterUser,
    String targetUser,
    String requesterBookTitle,
    String targetBookTitle,
    String status,
    LocalDateTime createdAt
) {
}
