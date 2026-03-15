package com.example.project.repository;

import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
	@Query("select o from Offer o join fetch o.book join fetch o.user where o.status = :status")
	List<Offer> findByStatusWithDetails(@Param("status") OfferStatus status);
}