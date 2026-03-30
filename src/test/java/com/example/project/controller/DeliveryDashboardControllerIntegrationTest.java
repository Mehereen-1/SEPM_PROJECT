package com.example.project.controller;

import com.example.project.entity.DeliveryOffer;

import com.example.project.entity.DeliveryOfferStatus;
import com.example.project.entity.Book;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.Offer;
import com.example.project.entity.User;
import com.example.project.repository.DeliveryOfferRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import com.example.project.service.DeliveryPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DeliveryDashboardController.
 * Tests API endpoints using MockMvc and mocked repositories.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DeliveryDashboardController Integration Tests")
class DeliveryDashboardControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DeliveryOfferRepository deliveryOfferRepository;

        @MockitoBean
        private ExchangeRequestRepository exchangeRequestRepository;

        @MockitoBean
        private UserRepository userRepository;

        @MockitoBean
        private SecurityUtil securityUtil;

        @MockitoBean
        private DeliveryPricingService deliveryPricingService;

        private User deliveryPartner;
        private User requester;
        private User receiver;
        private DeliveryOffer deliveryOffer;
        private ExchangeRequest exchangeRequest;

        @BeforeEach
        void setUp() {
                // Initialize test users
                deliveryPartner = new User();
                deliveryPartner.setId(1L);
                deliveryPartner.setName("John Delivery");
                deliveryPartner.setEmail("johndelivery@example.com");
                deliveryPartner.setLatitude(23.8103);
                deliveryPartner.setLongitude(90.4125);
                deliveryPartner.setAddress("123 Main St, Dhaka");

                requester = new User();
                requester.setId(2L);
                requester.setName("Alice Reader");
                requester.setEmail("alice@example.com");
                requester.setLatitude(23.7500);
                requester.setLongitude(90.3500);
                requester.setAddress("456 Oak St, Dhaka");

                receiver = new User();
                receiver.setId(3L);
                receiver.setName("Bob Reader");
                receiver.setEmail("bob@example.com");
                receiver.setLatitude(24.0000);
                receiver.setLongitude(90.5000);
                receiver.setAddress("789 Pine St, Dhaka");

                // Initialize exchange request
                exchangeRequest = new ExchangeRequest();
                exchangeRequest.setId(1L);
                exchangeRequest.setStatus(ExchangeRequestStatus.ACCEPTED);

                Offer requesterOffer = new Offer();
                requesterOffer.setId(1L);
                requesterOffer.setUser(requester);
                Book requesterBook = new Book();
                requesterBook.setTitle("Programming Book");
                requesterOffer.setBook(requesterBook);

                Offer receiverOffer = new Offer();
                receiverOffer.setId(2L);
                receiverOffer.setUser(receiver);
                Book receiverBook = new Book();
                receiverBook.setTitle("Novel Book");
                receiverOffer.setBook(receiverBook);

                exchangeRequest.setRequesterOffer(requesterOffer);
                exchangeRequest.setTargetOffer(receiverOffer);

                // Initialize delivery offer
                deliveryOffer = new DeliveryOffer();
                deliveryOffer.setId(1L);
                deliveryOffer.setExchangeRequest(exchangeRequest);
                deliveryOffer.setStatus(DeliveryOfferStatus.AVAILABLE);
                deliveryOffer.setDistanceKm(15.0);
                deliveryOffer.setDeliveryFee(30.0);
                deliveryOffer.setCreatedAt(LocalDateTime.now());
        }

        // ========== Dashboard Endpoint Tests ==========

        @Test
        @DisplayName("Should display delivery dashboard with available offers")
        @WithMockUser(username = "testuser", roles = "DELIVERY_PARTNER")
        void testDeliveryDashboard_Success() throws Exception {
                // Given
                List<DeliveryOffer> availableOffers = new ArrayList<>();
                availableOffers.add(deliveryOffer);

                when(deliveryOfferRepository.findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE))
                                .thenReturn(availableOffers);
                when(exchangeRequestRepository
                                .findByStatusWithoutDeliveryOfferWithDetails(eq(ExchangeRequestStatus.ACCEPTED)))
                                .thenReturn(new ArrayList<>());

                // When & Then
                mockMvc.perform(get("/delivery/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("delivery-dashboard"))
                                .andExpect(model().attributeExists("offers"));

                verify(deliveryOfferRepository, times(1)).findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Should display available offers page")
        @WithMockUser(username = "testuser", roles = "DELIVERY_PARTNER")
        void testOffersPage_Success() throws Exception {
                // Given
                List<DeliveryOffer> availableOffers = new ArrayList<>();
                availableOffers.add(deliveryOffer);

                when(deliveryOfferRepository.findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE))
                                .thenReturn(availableOffers);
                when(exchangeRequestRepository
                                .findByStatusWithoutDeliveryOfferWithDetails(eq(ExchangeRequestStatus.ACCEPTED)))
                                .thenReturn(new ArrayList<>());

                // When & Then
                mockMvc.perform(get("/delivery/offers"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("delivery-dashboard"))
                                .andExpect(model().attributeExists("offers"));
        }

        @Test
        @DisplayName("Should display pending deliveries for current user")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testPendingDeliveries_Success() throws Exception {
                // Given
                deliveryOffer.setStatus(DeliveryOfferStatus.PENDING);
                deliveryOffer.setAssignedDeliveryPartner(deliveryPartner);
                deliveryOffer.setAcceptedAt(LocalDateTime.now());

                List<DeliveryOffer> pendingOffers = new ArrayList<>();
                pendingOffers.add(deliveryOffer);

                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByAssigneeAndStatusesWithDetails(
                                1L,
                                Arrays.asList(
                                                DeliveryOfferStatus.ACCEPTED,
                                                DeliveryOfferStatus.PICKUP_STARTED,
                                                DeliveryOfferStatus.BOOK_PICKED,
                                                DeliveryOfferStatus.PENDING)))
                                .thenReturn(pendingOffers);

                // When & Then
                mockMvc.perform(get("/delivery/pending"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("delivery-dashboard"))
                                .andExpect(model().attributeExists("offers"));

                verify(deliveryOfferRepository, times(1))
                                .findByAssigneeAndStatusesWithDetails(eq(1L), anyList());
        }

        @Test
        @DisplayName("Should display completed deliveries for current user")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testCompletedDeliveries_Success() throws Exception {
                // Given
                deliveryOffer.setStatus(DeliveryOfferStatus.COMPLETED);
                deliveryOffer.setAssignedDeliveryPartner(deliveryPartner);
                deliveryOffer.setCompletedAt(LocalDateTime.now());

                List<DeliveryOffer> completedOffers = new ArrayList<>();
                completedOffers.add(deliveryOffer);

                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByAssigneeAndStatusWithDetails(1L, DeliveryOfferStatus.COMPLETED))
                                .thenReturn(completedOffers);

                // When & Then
                mockMvc.perform(get("/delivery/completed"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("delivery-dashboard"))
                                .andExpect(model().attributeExists("offers"));

                verify(deliveryOfferRepository, times(1))
                                .findByAssigneeAndStatusWithDetails(1L, DeliveryOfferStatus.COMPLETED);
        }

        // ========== Accept Offer Tests ==========

        @Test
        @DisplayName("Should accept available delivery offer and change status to PENDING")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testAcceptOffer_Success() throws Exception {
                // Given
                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));
                when(deliveryOfferRepository.save(any(DeliveryOffer.class))).thenReturn(deliveryOffer);

                // When & Then
                mockMvc.perform(post("/delivery/accept/1").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/delivery/pending"));

                verify(deliveryOfferRepository, times(1)).save(any(DeliveryOffer.class));
        }

        @Test
        @DisplayName("Should not accept offer when not logged in")
        void testAcceptOffer_NotLoggedIn() throws Exception {
                // Given - No authentication

                // When & Then
                mockMvc.perform(post("/delivery/accept/1").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login"));

                verify(deliveryOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return error when delivery offer not found")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testAcceptOffer_OfferNotFound() throws Exception {
                // Given
                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(post("/delivery/accept/999").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/delivery/offers"));
        }

        @Test
        @DisplayName("Should not accept offer that is already pending")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testAcceptOffer_AlreadyPending() throws Exception {
                // Given
                deliveryOffer.setStatus(DeliveryOfferStatus.PENDING);
                deliveryOffer.setAssignedDeliveryPartner(deliveryPartner);

                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));

                // When & Then
                mockMvc.perform(post("/delivery/accept/1").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/delivery/offers"));

                verify(deliveryOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not accept offer that is already completed")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testAcceptOffer_AlreadyCompleted() throws Exception {
                // Given
                deliveryOffer.setStatus(DeliveryOfferStatus.COMPLETED);

                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));

                // When & Then
                mockMvc.perform(post("/delivery/accept/1").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/delivery/offers"));

                verify(deliveryOfferRepository, never()).save(any());
        }

        // ========== Complete Offer Tests ==========

        @Test
        @DisplayName("Should complete pending delivery offer successfully")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testCompleteOffer_Success() throws Exception {
                // Given
                deliveryOffer.setStatus(DeliveryOfferStatus.PENDING);
                deliveryOffer.setAssignedDeliveryPartner(deliveryPartner);
                deliveryOffer.setAcceptedAt(LocalDateTime.now());
                deliveryOffer.setBookPickedAt(LocalDateTime.now());

                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));
                when(deliveryOfferRepository.save(any(DeliveryOffer.class))).thenReturn(deliveryOffer);

                // When & Then
                mockMvc.perform(post("/delivery/complete/1").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.status").value("COMPLETED"));

                verify(deliveryOfferRepository, times(1)).save(any(DeliveryOffer.class));
        }

        @Test
        @DisplayName("Should not complete offer when not logged in")
        void testCompleteOffer_NotLoggedIn() throws Exception {
                // When & Then
                mockMvc.perform(post("/delivery/complete/1").with(csrf()))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false));

                verify(deliveryOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return error when trying to complete non-existent offer")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testCompleteOffer_OfferNotFound() throws Exception {
                // Given
                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(post("/delivery/complete/999").with(csrf()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false));

                verify(deliveryOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not complete offer assigned to different user")
        @WithMockUser(username = "otheruser", roles = "DELIVERY_PARTNER")
        void testCompleteOffer_UnauthorizedUser() throws Exception {
                // Given
                User otherUser = new User();
                otherUser.setId(99L);
                otherUser.setName("Other User");
                otherUser.setEmail("otheruser@example.com");

                deliveryOffer.setStatus(DeliveryOfferStatus.PENDING);
                deliveryOffer.setAssignedDeliveryPartner(deliveryPartner); // Assigned to different user

                when(securityUtil.getCurrentUsername()).thenReturn("otheruser@example.com");
                when(userRepository.findByEmail("otheruser@example.com")).thenReturn(Optional.of(otherUser));
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));

                // When & Then
                mockMvc.perform(post("/delivery/complete/1").with(csrf()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false));

                verify(deliveryOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not complete offer that is not in PENDING status")
        @WithMockUser(username = "johndelivery", roles = "DELIVERY_PARTNER")
        void testCompleteOffer_NotPending() throws Exception {
                // Given
                deliveryOffer.setAssignedDeliveryPartner(deliveryPartner);
                deliveryOffer.setStatus(DeliveryOfferStatus.AVAILABLE);

                when(securityUtil.getCurrentUsername()).thenReturn("johndelivery@example.com");
                when(userRepository.findByEmail("johndelivery@example.com")).thenReturn(Optional.of(deliveryPartner));
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));

                // When & Then
                mockMvc.perform(post("/delivery/complete/1").with(csrf()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));

                verify(deliveryOfferRepository, never()).save(any());
        }

        // ========== Location View Tests ==========

        @Test
        @DisplayName("Should display delivery location map with distance and cost")
        @WithMockUser(username = "testuser", roles = "DELIVERY_PARTNER")
        void testFindLocation_Success() throws Exception {
                // Given
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));
                when(deliveryPricingService.estimateDistanceKm(any(), any(), any(), any()))
                                .thenReturn(15.0);
                when(deliveryPricingService.calculateCost(15.0)).thenReturn(30.0);

                // When & Then
                mockMvc.perform(get("/delivery/location/1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("delivery-map"))
                                .andExpect(model().attributeExists("deliveryOffer"))
                                .andExpect(model().attributeExists("senderName"))
                                .andExpect(model().attributeExists("receiverName"))
                                .andExpect(model().attributeExists("distanceKm"))
                                .andExpect(model().attributeExists("deliveryCost"));
        }

        @Test
        @DisplayName("Should redirect to offers page when location not found")
        @WithMockUser(username = "testuser", roles = "DELIVERY_PARTNER")
        void testFindLocation_OfferNotFound() throws Exception {
                // Given
                when(deliveryOfferRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(get("/delivery/location/999"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/delivery/offers"));
        }

        @Test
        @DisplayName("Should return location data as JSON for map")
        @WithMockUser(username = "testuser", roles = "DELIVERY_PARTNER")
        void testLocationData_ReturnsJson() throws Exception {
                // Given
                when(deliveryOfferRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(deliveryOffer));

                // When & Then
                mockMvc.perform(get("/delivery/location-data/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", notNullValue()));

                verify(deliveryOfferRepository, times(1)).findByIdWithDetails(1L);
        }
}
