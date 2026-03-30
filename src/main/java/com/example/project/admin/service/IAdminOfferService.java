package com.example.project.admin.service;

import com.example.project.admin.dto.AdminOfferResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IAdminOfferService {
    List<AdminOfferResponse> getAllOffers();

    ResponseEntity<?> deleteOffer(Long id);

    ResponseEntity<?> blockOffer(Long id);
}
