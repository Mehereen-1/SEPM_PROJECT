package com.example.project.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "delivery_offers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exchange_request_id"})
)
public class DeliveryOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_request_id", nullable = false)
    private ExchangeRequest exchangeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_delivery_partner_id")
    private User assignedDeliveryPartner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryOfferStatus status;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "delivery_fee")
    private Double deliveryFee;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column
    private LocalDateTime pickupStartedAt;

    @Column
    private LocalDateTime bookPickedAt;

    @Column
    private LocalDateTime completedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExchangeRequest getExchangeRequest() {
        return exchangeRequest;
    }

    public void setExchangeRequest(ExchangeRequest exchangeRequest) {
        this.exchangeRequest = exchangeRequest;
    }

    public User getAssignedDeliveryPartner() {
        return assignedDeliveryPartner;
    }

    public void setAssignedDeliveryPartner(User assignedDeliveryPartner) {
        this.assignedDeliveryPartner = assignedDeliveryPartner;
    }

    public DeliveryOfferStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryOfferStatus status) {
        this.status = status;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getPickupStartedAt() {
        return pickupStartedAt;
    }

    public void setPickupStartedAt(LocalDateTime pickupStartedAt) {
        this.pickupStartedAt = pickupStartedAt;
    }

    public LocalDateTime getBookPickedAt() {
        return bookPickedAt;
    }

    public void setBookPickedAt(LocalDateTime bookPickedAt) {
        this.bookPickedAt = bookPickedAt;
    }
}