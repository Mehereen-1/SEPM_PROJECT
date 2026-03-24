package com.example.project.controller;

import com.example.project.entity.ExchangeRequest;
import com.example.project.repository.ExchangeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/exchange-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExchangeRequestController {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @GetMapping
    public ResponseEntity<List<AdminExchangeRequestResponse>> getAllExchangeRequests() {
        List<AdminExchangeRequestResponse> response = exchangeRequestRepository.findAllWithDetails()
            .stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    private AdminExchangeRequestResponse toResponse(ExchangeRequest exchangeRequest) {
        return new AdminExchangeRequestResponse(
            exchangeRequest.getId(),
            exchangeRequest.getRequesterOffer().getUser().getName(),
            exchangeRequest.getTargetOffer().getUser().getName(),
            exchangeRequest.getRequesterOffer().getBook().getTitle(),
            exchangeRequest.getTargetOffer().getBook().getTitle(),
            exchangeRequest.getStatus().name(),
            exchangeRequest.getCreatedAt()
        );
    }

    record AdminExchangeRequestResponse(
        Long exchangeRequestId,
        String requesterUser,
        String targetUser,
        String requesterBookTitle,
        String targetBookTitle,
        String status,
        LocalDateTime createdAt
    ) {
    }
}
