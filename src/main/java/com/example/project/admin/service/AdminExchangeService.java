package com.example.project.admin.service;

import com.example.project.admin.dto.AdminExchangeRequestResponse;
import com.example.project.entity.ExchangeRequest;
import com.example.project.repository.ExchangeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminExchangeService {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    public List<AdminExchangeRequestResponse> getAllExchangeRequests() {
        return exchangeRequestRepository.findAllWithDetails()
            .stream()
            .map(this::toResponse)
            .toList();
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
}
