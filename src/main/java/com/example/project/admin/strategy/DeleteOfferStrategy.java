package com.example.project.admin.strategy;

import com.example.project.entity.Offer;
import com.example.project.repository.OfferImageRepository;
import com.example.project.repository.OfferRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class DeleteOfferStrategy implements AdminActionStrategy {

    private final OfferRepository offerRepository;
    private final OfferImageRepository offerImageRepository;

    public DeleteOfferStrategy(OfferRepository offerRepository, OfferImageRepository offerImageRepository) {
        this.offerRepository = offerRepository;
        this.offerImageRepository = offerImageRepository;
    }

    @Override
    public void execute(Long id) {
        Optional<Offer> offerResult = offerRepository.findById(id);
        if (offerResult.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Offer not found.");
        }

        try {
            offerImageRepository.deleteByOffer_Id(id);
            offerRepository.delete(offerResult.get());
        } catch (Exception ex) {
            throw new ResponseStatusException(CONFLICT, "Offer cannot be deleted because it is referenced by active exchange data.");
        }
    }
}
