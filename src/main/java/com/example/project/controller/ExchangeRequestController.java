package com.example.project.controller;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryOfferStatus;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.User;
import com.example.project.notification.service.NotificationService;
import com.example.project.repository.DeliveryOfferRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.OfferRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/exchange-requests")
public class ExchangeRequestController {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryOfferRepository deliveryOfferRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<?> createExchangeRequest(@RequestBody CreateExchangeRequest request) {
        if (request.requesterOfferId() == null || request.targetOfferId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "requesterOfferId and targetOfferId are required."));
        }

        Optional<User> currentUserResult = getCurrentUser();
        if (currentUserResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User is not authenticated."));
        }

        Optional<Offer> requesterOfferResult = offerRepository.findById(request.requesterOfferId());
        if (requesterOfferResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Requester offer not found."));
        }

        Optional<Offer> targetOfferResult = offerRepository.findById(request.targetOfferId());
        if (targetOfferResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Target offer not found."));
        }

        Offer requesterOffer = requesterOfferResult.get();
        Offer targetOffer = targetOfferResult.get();
        User currentUser = currentUserResult.get();

        if (!requesterOffer.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Requester offer must belong to the current user."));
        }

        if (requesterOffer.getStatus() != OfferStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Requester offer must be ACTIVE."));
        }

        if (targetOffer.getStatus() != OfferStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Target offer must be ACTIVE."));
        }

        if (targetOffer.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You cannot request your own offer."));
        }

        if (exchangeRequestRepository.existsByRequesterOffer_IdAndTargetOffer_Id(request.requesterOfferId(), request.targetOfferId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Duplicate exchange request for this offer pair is not allowed."));
        }

        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequesterOffer(requesterOffer);
        exchangeRequest.setTargetOffer(targetOffer);
        exchangeRequest.setStatus(ExchangeRequestStatus.PENDING);
        exchangeRequest.setCreatedAt(LocalDateTime.now());

        ExchangeRequest saved = exchangeRequestRepository.save(exchangeRequest);
        notificationService.publishExchangeRequestSent(saved, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptExchangeRequest(@PathVariable Long id) {
        Optional<User> currentUserResult = getCurrentUser();
        if (currentUserResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User is not authenticated."));
        }

        Optional<ExchangeRequest> exchangeRequestResult = exchangeRequestRepository.findByIdWithDetails(id);
        if (exchangeRequestResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Exchange request not found."));
        }

        ExchangeRequest exchangeRequest = exchangeRequestResult.get();
        User currentUser = currentUserResult.get();

        if (!exchangeRequest.getTargetOffer().getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Only the owner of the target offer can accept this request."));
        }

        if (exchangeRequest.getStatus() != ExchangeRequestStatus.PENDING) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Only PENDING requests can be accepted."));
        }

        exchangeRequest.setStatus(ExchangeRequestStatus.ACCEPTED);
        exchangeRequest.getRequesterOffer().setStatus(OfferStatus.RESERVED);
        exchangeRequest.getTargetOffer().setStatus(OfferStatus.RESERVED);

        offerRepository.save(exchangeRequest.getRequesterOffer());
        offerRepository.save(exchangeRequest.getTargetOffer());
        ExchangeRequest updated = exchangeRequestRepository.save(exchangeRequest);
        notificationService.publishExchangeAccepted(updated, currentUser.getId());
        createDeliveryOfferIfMissing(updated);

        return ResponseEntity.ok(toResponse(updated));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectExchangeRequest(@PathVariable Long id) {
        Optional<User> currentUserResult = getCurrentUser();
        if (currentUserResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User is not authenticated."));
        }

        Optional<ExchangeRequest> exchangeRequestResult = exchangeRequestRepository.findByIdWithDetails(id);
        if (exchangeRequestResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Exchange request not found."));
        }

        ExchangeRequest exchangeRequest = exchangeRequestResult.get();
        User currentUser = currentUserResult.get();

        if (!exchangeRequest.getTargetOffer().getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Only the owner of the target offer can reject this request."));
        }

        if (exchangeRequest.getStatus() != ExchangeRequestStatus.PENDING) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Only PENDING requests can be rejected."));
        }

        exchangeRequest.setStatus(ExchangeRequestStatus.REJECTED);
        ExchangeRequest updated = exchangeRequestRepository.save(exchangeRequest);
        notificationService.publishExchangeRejected(updated, currentUser.getId());

        return ResponseEntity.ok(toResponse(updated));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests() {
        Optional<User> currentUserResult = getCurrentUser();
        if (currentUserResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User is not authenticated."));
        }

        Long userId = currentUserResult.get().getId();
        List<ExchangeRequestDetailsResponse> sentRequests = exchangeRequestRepository.findSentByUserIdWithDetails(userId)
            .stream()
            .map(this::toResponse)
            .toList();

        List<ExchangeRequestDetailsResponse> receivedRequests = exchangeRequestRepository.findReceivedByUserIdWithDetails(userId)
            .stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(new MyExchangeRequestsResponse(sentRequests, receivedRequests));
    }

    private Optional<User> getCurrentUser() {
        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            return Optional.empty();
        }
        return userRepository.findByEmail(currentUsername);
    }

    private void createDeliveryOfferIfMissing(ExchangeRequest exchangeRequest) {
        if (exchangeRequest == null || exchangeRequest.getId() == null) {
            return;
        }

        if (deliveryOfferRepository.existsByExchangeRequest_Id(exchangeRequest.getId())) {
            return;
        }

        DeliveryOffer deliveryOffer = new DeliveryOffer();
        deliveryOffer.setExchangeRequest(exchangeRequest);
        deliveryOffer.setStatus(DeliveryOfferStatus.AVAILABLE);
        deliveryOffer.setDeliveryFee(0.0d);
        deliveryOffer.setCreatedAt(LocalDateTime.now());
        DeliveryOffer created = deliveryOfferRepository.save(deliveryOffer);
        notificationService.publishDeliveryCreated(created, null);
    }

    private ExchangeRequestDetailsResponse toResponse(ExchangeRequest exchangeRequest) {
        return new ExchangeRequestDetailsResponse(
            exchangeRequest.getId(),
            exchangeRequest.getRequesterOffer().getUser().getName(),
            exchangeRequest.getTargetOffer().getUser().getName(),
            exchangeRequest.getRequesterOffer().getBook().getTitle(),
            exchangeRequest.getTargetOffer().getBook().getTitle(),
            exchangeRequest.getStatus().name(),
            exchangeRequest.getCreatedAt()
        );
    }

    record CreateExchangeRequest(
        Long requesterOfferId,
        Long targetOfferId
    ) {
    }

    record ExchangeRequestDetailsResponse(
        Long exchangeRequestId,
        String requesterUser,
        String targetUser,
        String requesterBookTitle,
        String targetBookTitle,
        String status,
        LocalDateTime createdAt
    ) {
    }

    record MyExchangeRequestsResponse(
        List<ExchangeRequestDetailsResponse> sentRequests,
        List<ExchangeRequestDetailsResponse> receivedRequests
    ) {
    }
}
