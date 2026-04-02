package com.example.project.service;

import org.springframework.stereotype.Service;

@Service
public class DeliveryPricingService {

    private static final double COST_PER_KM = 2.0;

    public Double calculateCost(Double distanceKm) {
        if (distanceKm == null || distanceKm < 0) {
            return null;
        }
        return distanceKm * COST_PER_KM;
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
}
