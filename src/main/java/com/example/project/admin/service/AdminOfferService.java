package com.example.project.admin.service;

import com.example.project.admin.dto.AdminOfferResponse;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.repository.OfferImageRepository;
import com.example.project.repository.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminOfferService implements IAdminOfferService {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferImageRepository offerImageRepository;

    public List<AdminOfferResponse> getAllOffers() {
        return offerRepository.findAllWithDetails()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ResponseEntity<?> deleteOffer(Long id) {
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

    public ResponseEntity<?> blockOffer(Long id) {
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
}
