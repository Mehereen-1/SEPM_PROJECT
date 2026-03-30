package com.example.project.admin.strategy;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryOfferStatus;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.OfferStatus;
import com.example.project.notification.service.NotificationService;
import com.example.project.repository.DeliveryOfferRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.OfferRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class ApproveExchangeStrategy implements AdminActionStrategy {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final OfferRepository offerRepository;
    private final DeliveryOfferRepository deliveryOfferRepository;
    private final NotificationService notificationService;

    public ApproveExchangeStrategy(
        ExchangeRequestRepository exchangeRequestRepository,
        OfferRepository offerRepository,
        DeliveryOfferRepository deliveryOfferRepository,
        NotificationService notificationService
    ) {
        this.exchangeRequestRepository = exchangeRequestRepository;
        this.offerRepository = offerRepository;
        this.deliveryOfferRepository = deliveryOfferRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void execute(Long id) {
        Optional<ExchangeRequest> exchangeRequestResult = exchangeRequestRepository.findByIdWithDetails(id);
        if (exchangeRequestResult.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Exchange request not found.");
        }

        ExchangeRequest exchangeRequest = exchangeRequestResult.get();
        if (exchangeRequest.getStatus() != ExchangeRequestStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING requests can be approved.");
        }

        exchangeRequest.setStatus(ExchangeRequestStatus.ACCEPTED);
        exchangeRequest.getRequesterOffer().setStatus(OfferStatus.RESERVED);
        exchangeRequest.getTargetOffer().setStatus(OfferStatus.RESERVED);

        offerRepository.save(exchangeRequest.getRequesterOffer());
        offerRepository.save(exchangeRequest.getTargetOffer());
        ExchangeRequest updated = exchangeRequestRepository.save(exchangeRequest);
        notificationService.publishExchangeAccepted(updated, null);

        if (!deliveryOfferRepository.existsByExchangeRequest_Id(updated.getId())) {
            DeliveryOffer deliveryOffer = new DeliveryOffer();
            deliveryOffer.setExchangeRequest(updated);
            deliveryOffer.setStatus(DeliveryOfferStatus.AVAILABLE);
            deliveryOffer.setDeliveryFee(0.0d);
            deliveryOffer.setCreatedAt(LocalDateTime.now());
            DeliveryOffer created = deliveryOfferRepository.save(deliveryOffer);
            notificationService.publishDeliveryCreated(created, null);
        }
    }
}
