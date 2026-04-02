package com.example.project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeliveryPricingService.
 * Tests cost calculation and distance estimation logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryPricingService Unit Tests")
class DeliveryPricingServiceTest {

    private DeliveryPricingService deliveryPricingService;

    @BeforeEach
    void setUp() {
        deliveryPricingService = new DeliveryPricingService();
    }

    // ========== Cost Calculation Tests ==========

    @Test
    @DisplayName("Should calculate cost correctly with valid distance")
    void testCalculateCost_WithValidDistance() {
        // Given
        Double distanceKm = 10.0;
        Double expectedCost = 20.0; // 10.0 * 2.0

        // When
        Double actualCost = deliveryPricingService.calculateCost(distanceKm);

        // Then
        assertEquals(expectedCost, actualCost);
    }

    @Test
    @DisplayName("Should return zero cost for zero distance")
    void testCalculateCost_WithZeroDistance() {
        // Given
        Double distanceKm = 0.0;
        Double expectedCost = 0.0;

        // When
        Double actualCost = deliveryPricingService.calculateCost(distanceKm);

        // Then
        assertEquals(expectedCost, actualCost);
    }

    @Test
    @DisplayName("Should return null for negative distance")
    void testCalculateCost_WithNegativeDistance() {
        // Given
        Double distanceKm = -5.0;

        // When
        Double actualCost = deliveryPricingService.calculateCost(distanceKm);

        // Then
        assertNull(actualCost);
    }

    @Test
    @DisplayName("Should return null for null distance")
    void testCalculateCost_WithNullDistance() {
        // Given
        Double distanceKm = null;

        // When
        Double actualCost = deliveryPricingService.calculateCost(distanceKm);

        // Then
        assertNull(actualCost);
    }

    @Test
    @DisplayName("Should calculate cost with decimal distances")
    void testCalculateCost_WithDecimalDistance() {
        // Given
        Double distanceKm = 5.5;
        Double expectedCost = 11.0; // 5.5 * 2.0

        // When
        Double actualCost = deliveryPricingService.calculateCost(distanceKm);

        // Then
        assertEquals(expectedCost, actualCost);
    }

    @Test
    @DisplayName("Should calculate cost correctly for large distances")
    void testCalculateCost_WithLargeDistance() {
        // Given
        Double distanceKm = 1000.0;
        Double expectedCost = 2000.0;

        // When
        Double actualCost = deliveryPricingService.calculateCost(distanceKm);

        // Then
        assertEquals(expectedCost, actualCost);
    }

    // ========== Distance Estimation Tests ==========

    @Test
    @DisplayName("Should estimate distance correctly between two coordinates")
    void testEstimateDistanceKm_WithValidCoordinates() {
        // Given - Dhaka, Bangladesh coordinates
        Double lat1 = 23.8103;
        Double lng1 = 90.4125;
        Double lat2 = 23.8103;
        Double lng2 = 90.4125;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then - Same coordinates should give ~0 distance
        assertNotNull(distance);
        assertTrue(distance < 0.01); // Nearly zero with small tolerance
    }

    @Test
    @DisplayName("Should calculate positive distance between different coordinates")
    void testEstimateDistanceKm_WithDifferentCoordinates() {
        // Given - Two different locations
        Double lat1 = 23.8103;
        Double lng1 = 90.4125;
        Double lat2 = 24.9050; // ~120 km north
        Double lng2 = 91.8354;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then - Should return a positive distance
        assertNotNull(distance);
        assertTrue(distance > 0);
        assertTrue(distance < 200); // Should be roughly 100-120 km
    }

    @Test
    @DisplayName("Should return null when any coordinate is null")
    void testEstimateDistanceKm_WithNullLatitude1() {
        // Given
        Double lat1 = null;
        Double lng1 = 90.4125;
        Double lat2 = 23.8103;
        Double lng2 = 90.4125;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then
        assertNull(distance);
    }

    @Test
    @DisplayName("Should return null when longitude is null")
    void testEstimateDistanceKm_WithNullLongitude1() {
        // Given
        Double lat1 = 23.8103;
        Double lng1 = null;
        Double lat2 = 23.8103;
        Double lng2 = 90.4125;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then
        assertNull(distance);
    }

    @Test
    @DisplayName("Should return null when destination latitude is null")
    void testEstimateDistanceKm_WithNullLatitude2() {
        // Given
        Double lat1 = 23.8103;
        Double lng1 = 90.4125;
        Double lat2 = null;
        Double lng2 = 90.4125;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then
        assertNull(distance);
    }

    @Test
    @DisplayName("Should return null when destination longitude is null")
    void testEstimateDistanceKm_WithNullLongitude2() {
        // Given
        Double lat1 = 23.8103;
        Double lng1 = 90.4125;
        Double lat2 = 23.8103;
        Double lng2 = null;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then
        assertNull(distance);
    }

    @Test
    @DisplayName("Should return null when all coordinates are null")
    void testEstimateDistanceKm_WithAllNullCoordinates() {
        // Given
        Double lat1 = null;
        Double lng1 = null;
        Double lat2 = null;
        Double lng2 = null;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then
        assertNull(distance);
    }

    @Test
    @DisplayName("Should calculate distance using Haversine formula correctly")
    void testEstimateDistanceKm_HaversineAccuracy() {
        // Given - Known coordinates with approximate known distance
        // Dhaka to Sylhet is roughly 175 km
        Double lat1 = 23.8103;
        Double lng1 = 90.4125;
        Double lat2 = 24.8915;
        Double lng2 = 91.8688;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then - Should be approximately 175 km (with reasonable margin)
        assertNotNull(distance);
        assertTrue(distance > 150 && distance < 200);
    }

    @Test
    @DisplayName("Should handle extreme latitude values")
    void testEstimateDistanceKm_WithExtremeLatitudes() {
        // Given - North pole to equator
        Double lat1 = 90.0;
        Double lng1 = 0.0;
        Double lat2 = 0.0;
        Double lng2 = 0.0;

        // When
        Double distance = deliveryPricingService.estimateDistanceKm(lat1, lng1, lat2, lng2);

        // Then - Should be roughly 10,000 km (quarter of Earth's circumference)
        assertNotNull(distance);
        assertTrue(distance > 9000 && distance < 11000);
    }
}
