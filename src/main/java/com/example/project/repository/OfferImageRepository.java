package com.example.project.repository;

import com.example.project.entity.OfferImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferImageRepository extends JpaRepository<OfferImage, Long> {
	List<OfferImage> findByOfferIdIn(List<Long> offerIds);
}