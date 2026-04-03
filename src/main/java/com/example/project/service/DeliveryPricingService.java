package com.example.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class DeliveryPricingService {

    private static final double COST_PER_KM = 2.0;

    public RouteMetrics resolveRouteMetrics(
        Double storedDistanceKm,
        Double storedDeliveryCost,
        Double lat1,
        Double lng1,
        Double lat2,
        Double lng2
    ) {
        Double distanceKm = normalizeDistanceKm(storedDistanceKm);
        if (distanceKm == null) {
            distanceKm = normalizeDistanceKm(estimateDistanceKm(lat1, lng1, lat2, lng2));
        }

        Double deliveryCost = calculateCost(distanceKm);
        if (deliveryCost == null) {
            deliveryCost = normalizeMoney(storedDeliveryCost);
        }

        return new RouteMetrics(distanceKm, deliveryCost);
    }

    public Double calculateCost(Double distanceKm) {
        Double normalizedDistance = normalizeDistanceKm(distanceKm);
        if (normalizedDistance == null) {
            return null;
        }
        return normalizeMoney(normalizedDistance * COST_PER_KM);
    }

    public double getCostPerKm() {
        return COST_PER_KM;
    }

    public Double normalizeDistanceKm(Double distanceKm) {
        if (distanceKm == null || distanceKm < 0) {
            return null;
        }
        return roundToTwoDecimals(distanceKm);
    }

    public Double normalizeMoney(Double amount) {
        if (amount == null || amount < 0) {
            return null;
        }
        return roundToTwoDecimals(amount);
    }

    public Double estimateDistanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }

        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private Double roundToTwoDecimals(Double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public record RouteMetrics(
        Double distanceKm,
        Double deliveryCost
    ) {
    }
}
