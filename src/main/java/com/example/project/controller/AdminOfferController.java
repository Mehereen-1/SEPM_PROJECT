package com.example.project.controller;

import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.repository.OfferImageRepository;
import com.example.project.repository.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/offers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOfferController {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferImageRepository offerImageRepository;

    @GetMapping
    public ResponseEntity<List<AdminOfferResponse>> getAllOffers() {
        List<AdminOfferResponse> offers = offerRepository.findAllWithDetails()
            .stream()
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(offers);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOffer(@PathVariable Long id) {
        Optional<Offer> offerResult = offerRepository.findById(id);
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Offer not found."));
        }

        try {
            offerImageRepository.deleteByOffer_Id(id);
            offerRepository.delete(offerResult.get());
            return ResponseEntity.ok(Map.of("message", "Offer deleted successfully."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Offer cannot be deleted because it is referenced by active exchange data."));
        }
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<?> blockOffer(@PathVariable Long id) {
        Optional<Offer> offerResult = offerRepository.findById(id);
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Offer not found."));
        }

        Offer offer = offerResult.get();
        offer.setStatus(OfferStatus.BLOCKED);
        Offer saved = offerRepository.save(offer);

        return ResponseEntity.ok(toResponse(saved));
    }

    private AdminOfferResponse toResponse(Offer offer) {
        return new AdminOfferResponse(
            offer.getId(),
            offer.getBook().getTitle(),
            offer.getUser().getName(),
            offer.getCondition(),
            offer.getStatus().name(),
            offer.getCreatedAt()
        );
    }

    record AdminOfferResponse(
        Long offerId,
        String bookTitle,
        String ownerName,
        String condition,
        String status,
        LocalDateTime createdAt
    ) {
    }
}
