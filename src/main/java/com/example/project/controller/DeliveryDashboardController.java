package com.example.project.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryOfferStatus;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.User;
import com.example.project.notification.service.NotificationService;
import com.example.project.repository.DeliveryOfferRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import com.example.project.service.DeliveryPricingService;

@Controller
public class DeliveryDashboardController {

    private static final Logger LOGGER = Logger.getLogger(DeliveryDashboardController.class.getName());

    @Autowired
    private DeliveryOfferRepository deliveryOfferRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private DeliveryPricingService deliveryPricingService;

    @Autowired
    private NotificationService notificationService;

    private static final double DEFAULT_LAT = 23.8103;
    private static final double DEFAULT_LNG = 90.4125;

    @GetMapping("/delivery/dashboard")
    public String deliveryDashboard(Model model) {
        ensureAvailableOffersAreGenerated();
        List<DeliveryOfferCardView> cards = buildCards(deliveryOfferRepository.findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE));
        populateModel(model, cards, "Delivery Dashboard", "Book Exchange Offers", "Browse and accept exchange deliveries from readers.", false);
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/offers")
    public String offers(Model model) {
        ensureAvailableOffersAreGenerated();
        List<DeliveryOfferCardView> cards = buildCards(deliveryOfferRepository.findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE));
        populateModel(model, cards, "Available Offers", "Book Exchange Offers", "Live exchange tasks waiting for a delivery partner.", false);
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/available")
    public String available(Model model) {
        return offers(model);
    }

    @GetMapping("/delivery/pending")
    public String pending(Model model) {
        Optional<User> currentUser = getCurrentUser();
        List<DeliveryOfferCardView> cards = currentUser
            .map(user -> deliveryOfferRepository.findByAssigneeAndStatusesWithDetails(
                user.getId(),
                Arrays.asList(
                    DeliveryOfferStatus.ACCEPTED,
                    DeliveryOfferStatus.PICKUP_STARTED,
                    DeliveryOfferStatus.BOOK_PICKED,
                    DeliveryOfferStatus.PENDING
                )
            ))
            .orElseGet(List::of)
            .stream()
            .map(this::toCard)
            .toList();

        populateModel(model, cards, "Pending Deliveries", "Pending Delivery Offers", "Deliveries you accepted and are currently handling.", false);
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/completed")
    public String completed(Model model) {
        Optional<User> currentUser = getCurrentUser();
        List<DeliveryOfferCardView> cards = currentUser
            .map(user -> deliveryOfferRepository.findByAssigneeAndStatusWithDetails(user.getId(), DeliveryOfferStatus.COMPLETED))
            .orElseGet(List::of)
            .stream()
            .map(this::toCard)
            .toList();

        populateModel(model, cards, "Completed Deliveries", "Completed Delivery Offers", "Completed exchange deliveries assigned to you.", true);
        return "delivery-dashboard";
    }

    @PostMapping("/delivery/accept/{id}")
    public String acceptOffer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> currentUser = getCurrentUser();
            if (currentUser.isEmpty()) {
                redirectAttributes.addFlashAttribute("deliveryMessage", "You must be logged in to accept delivery offers.");
                return "redirect:/login";
            }

            Optional<DeliveryOffer> offerResult = deliveryOfferRepository.findByIdWithDetails(id);
            if (offerResult.isEmpty()) {
                redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer not found.");
                return "redirect:/delivery/offers";
            }

            DeliveryOffer offer = offerResult.get();
            if (offer.getStatus() != DeliveryOfferStatus.AVAILABLE) {
                redirectAttributes.addFlashAttribute("deliveryMessage", "This delivery offer is no longer available.");
                return "redirect:/delivery/offers";
            }

            offer.setAssignedDeliveryPartner(currentUser.get());
            // Persist with legacy-compatible status to avoid DB enum/check constraint conflicts.
            offer.setStatus(DeliveryOfferStatus.PENDING);
            offer.setAcceptedAt(LocalDateTime.now());
            deliveryOfferRepository.save(offer);
            safePublish(() -> notificationService.publishDeliveryAssigned(offer, currentUser.get().getId()), "publishDeliveryAssigned", offer.getId());

            redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer accepted successfully.");
            return "redirect:/delivery/pending";
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Accept delivery failed for offer " + id, ex);
            redirectAttributes.addFlashAttribute("deliveryMessage", "Could not accept this offer right now. Please try again.");
            return "redirect:/delivery/offers";
        }
    }

    @GetMapping("/delivery/accept/{id}")
    public String acceptOfferGetFallback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("deliveryMessage", "Please use the Accept Delivery button from the dashboard.");
        return "redirect:/delivery/offers";
    }

    @PostMapping("/delivery/pickup-start/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startPickup(@PathVariable Long id) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "You must be logged in to start pickup."));
        }

        Optional<DeliveryOffer> offerResult = findOwnedOfferForLifecycle(id, currentUser.get().getId());
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Delivery offer not found for your account."));
        }

        DeliveryOffer offer = offerResult.get();
        if (offer.getStatus() != DeliveryOfferStatus.PENDING || offer.getPickupStartedAt() != null) {
            return ResponseEntity.badRequest()
            .body(Map.of("success", false, "message", "Pickup can only be started from accepted delivery status."));
        }

        offer.setPickupStartedAt(LocalDateTime.now());
        deliveryOfferRepository.save(offer);
        safePublish(() -> notificationService.publishPickupStarted(offer, currentUser.get().getId()), "publishPickupStarted", offer.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "status", "PICKUP_STARTED",
            "message", "Pickup started successfully"
        ));
    }

    @GetMapping("/delivery/pickup-start/{id}")
    public String startPickupGetFallback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("deliveryMessage", "Use the Start Pickup button from Pending deliveries.");
        return "redirect:/delivery/pending";
    }

    @PostMapping("/delivery/book-picked/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markBookPicked(@PathVariable Long id) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "You must be logged in to mark book picked."));
        }

        Optional<DeliveryOffer> offerResult = findOwnedOfferForLifecycle(id, currentUser.get().getId());
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Delivery offer not found for your account."));
        }

        DeliveryOffer offer = offerResult.get();
        if (offer.getStatus() != DeliveryOfferStatus.PENDING || offer.getPickupStartedAt() == null || offer.getBookPickedAt() != null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Book can be marked picked only after pickup has started."));
        }

        offer.setBookPickedAt(LocalDateTime.now());
        deliveryOfferRepository.save(offer);
        safePublish(() -> notificationService.publishBookPicked(offer, currentUser.get().getId()), "publishBookPicked", offer.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "status", "BOOK_PICKED",
            "message", "Book picked successfully"
        ));
    }

    @GetMapping("/delivery/book-picked/{id}")
    public String markBookPickedGetFallback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("deliveryMessage", "Use the Book Picked button from Pending deliveries.");
        return "redirect:/delivery/pending";
    }

    @PostMapping("/delivery/complete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeOffer(@PathVariable Long id) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "You must be logged in to complete delivery offers."));
        }

        Optional<DeliveryOffer> offerResult = findOwnedOfferForLifecycle(id, currentUser.get().getId());
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Delivery offer not found for your account."));
        }

        DeliveryOffer offer = offerResult.get();
        if (offer.getStatus() != DeliveryOfferStatus.PENDING || offer.getBookPickedAt() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Delivery can only be completed after book is marked picked."));
        }

        offer.setStatus(DeliveryOfferStatus.COMPLETED);
        offer.setCompletedAt(LocalDateTime.now());
        deliveryOfferRepository.save(offer);
        safePublish(() -> notificationService.publishDeliveryCompleted(offer, currentUser.get().getId()), "publishDeliveryCompleted", offer.getId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "status", offer.getStatus().name(),
            "message", "Delivery completed"
        ));
    }

    @GetMapping("/delivery/complete/{id}")
    public String completeOfferGetFallback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("deliveryMessage", "Use the Mark Delivered button from Pending deliveries.");
        return "redirect:/delivery/pending";
    }

    @GetMapping("/delivery/location/{id}")
    public String findLocation(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<DeliveryOffer> offerResult = deliveryOfferRepository.findByIdWithDetails(id);
        if (offerResult.isEmpty()) {
            redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer not found.");
            return "redirect:/delivery/offers";
        }

        DeliveryOffer offer = offerResult.get();
        User sender = offer.getExchangeRequest().getRequesterOffer().getUser();
        User receiver = offer.getExchangeRequest().getTargetOffer().getUser();

        Coordinate senderCoordinate = resolveCoordinate(sender);
        Coordinate receiverCoordinate = resolveCoordinate(receiver);

        Double distanceKm = offer.getDistanceKm();
        if (distanceKm == null) {
            distanceKm = deliveryPricingService.estimateDistanceKm(
                senderCoordinate.latitude(),
                senderCoordinate.longitude(),
                receiverCoordinate.latitude(),
                receiverCoordinate.longitude()
            );
        }
        Double deliveryCost = deliveryPricingService.calculateCost(distanceKm);

        model.addAttribute("deliveryOffer", offer);
        model.addAttribute("senderName", sender.getName());
        model.addAttribute("receiverName", receiver.getName());
        model.addAttribute("senderLat", senderCoordinate.latitude());
        model.addAttribute("senderLng", senderCoordinate.longitude());
        model.addAttribute("receiverLat", receiverCoordinate.latitude());
        model.addAttribute("receiverLng", receiverCoordinate.longitude());
        model.addAttribute("senderAddress", sender.getAddress());
        model.addAttribute("receiverAddress", receiver.getAddress());
        model.addAttribute("distanceKm", distanceKm);
        model.addAttribute("deliveryCost", deliveryCost);
        model.addAttribute("hasExactSenderLocation", sender.getLatitude() != null && sender.getLongitude() != null);
        model.addAttribute("hasExactReceiverLocation", receiver.getLatitude() != null && receiver.getLongitude() != null);
        model.addAttribute("fallbackLat", DEFAULT_LAT);
        model.addAttribute("fallbackLng", DEFAULT_LNG);

        // Keep stored values in sync if we calculated estimates server-side.
        if (offer.getDistanceKm() == null || offer.getDeliveryFee() == null) {
            offer.setDistanceKm(distanceKm);
            offer.setDeliveryFee(deliveryCost);
            deliveryOfferRepository.save(offer);
        }

        return "delivery-map";
    }

    @GetMapping("/delivery/location-data/{id}")
    @ResponseBody
    public Map<String, Object> locationData(@PathVariable Long id) {
        DeliveryOffer offer = deliveryOfferRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery offer not found"));

        User sender = offer.getExchangeRequest().getRequesterOffer().getUser();
        User receiver = offer.getExchangeRequest().getTargetOffer().getUser();

        Coordinate senderCoordinate = resolveCoordinate(sender);
        Coordinate receiverCoordinate = resolveCoordinate(receiver);
        Double distanceKm = offer.getDistanceKm() != null
            ? offer.getDistanceKm()
            : deliveryPricingService.estimateDistanceKm(
                senderCoordinate.latitude(),
                senderCoordinate.longitude(),
                receiverCoordinate.latitude(),
                receiverCoordinate.longitude()
            );

        Map<String, Object> payload = new HashMap<>();
        payload.put("senderLat", senderCoordinate.latitude());
        payload.put("senderLng", senderCoordinate.longitude());
        payload.put("receiverLat", receiverCoordinate.latitude());
        payload.put("receiverLng", receiverCoordinate.longitude());
        payload.put("distanceKm", distanceKm);
        payload.put("cost", deliveryPricingService.calculateCost(distanceKm));
        return payload;
    }

    private Coordinate resolveCoordinate(User user) {
        if (user == null || user.getLatitude() == null || user.getLongitude() == null) {
            return new Coordinate(DEFAULT_LAT, DEFAULT_LNG);
        }

        return new Coordinate(user.getLatitude(), user.getLongitude());
    }

    private void populateModel(
        Model model,
        List<DeliveryOfferCardView> cards,
        String breadcrumbTitle,
        String pageHeading,
        String pageDescription,
        boolean completedView
    ) {
        model.addAttribute("offers", cards);
        model.addAttribute("breadcrumbTitle", breadcrumbTitle);
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("completedView", completedView);
    }

    private Optional<DeliveryOffer> findOwnedOfferForLifecycle(Long offerId, Long deliveryPartnerId) {
        return deliveryOfferRepository.findByIdWithDetails(offerId)
            .filter(offer -> offer.getAssignedDeliveryPartner() != null
                && deliveryPartnerId.equals(offer.getAssignedDeliveryPartner().getId()));
    }

    private List<DeliveryOfferCardView> buildCards(List<DeliveryOffer> offers) {
        return offers.stream().map(this::toCard).toList();
    }

    private DeliveryOfferCardView toCard(DeliveryOffer offer) {
        String requesterName = offer.getExchangeRequest().getRequesterOffer().getUser().getName();
        String targetName = offer.getExchangeRequest().getTargetOffer().getUser().getName();

        String headline = requesterName + " wants to exchange a book with " + targetName;
        String bookPair = offer.getExchangeRequest().getRequesterOffer().getBook().getTitle()
            + " <-> "
            + offer.getExchangeRequest().getTargetOffer().getBook().getTitle();

        String lifecycleStatus = resolveLifecycleStatus(offer);

        return new DeliveryOfferCardView(
            offer.getId(),
            headline,
            requesterName,
            targetName,
            bookPair,
            offer.getDistanceKm(),
            lifecycleStatus,
            offer.getAssignedDeliveryPartner() != null ? offer.getAssignedDeliveryPartner().getName() : null
        );
    }

    private String resolveLifecycleStatus(DeliveryOffer offer) {
        if (offer.getStatus() == DeliveryOfferStatus.COMPLETED) {
            return DeliveryOfferStatus.COMPLETED.name();
        }
        if (offer.getBookPickedAt() != null) {
            return DeliveryOfferStatus.BOOK_PICKED.name();
        }
        if (offer.getPickupStartedAt() != null) {
            return DeliveryOfferStatus.PICKUP_STARTED.name();
        }
        if (offer.getAssignedDeliveryPartner() != null && offer.getAcceptedAt() != null) {
            return DeliveryOfferStatus.ACCEPTED.name();
        }
        return offer.getStatus().name();
    }

    private Optional<User> getCurrentUser() {
        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            return Optional.empty();
        }
        return userRepository.findByEmail(currentUsername);
    }

    private void ensureAvailableOffersAreGenerated() {
        List<ExchangeRequest> acceptedWithoutDelivery = exchangeRequestRepository
            .findByStatusWithoutDeliveryOfferWithDetails(ExchangeRequestStatus.ACCEPTED);

        for (ExchangeRequest exchangeRequest : acceptedWithoutDelivery) {
            if (deliveryOfferRepository.existsByExchangeRequest_Id(exchangeRequest.getId())) {
                continue;
            }

            DeliveryOffer offer = new DeliveryOffer();
            offer.setExchangeRequest(exchangeRequest);
            offer.setStatus(DeliveryOfferStatus.AVAILABLE);
            offer.setDeliveryFee(0.0d);
            offer.setCreatedAt(LocalDateTime.now());
            DeliveryOffer created = deliveryOfferRepository.save(offer);
            safePublish(() -> notificationService.publishDeliveryCreated(created, null), "publishDeliveryCreated", created.getId());
        }
    }

    private void safePublish(Runnable publishAction, String actionName, Long offerId) {
        try {
            publishAction.run();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Notification action failed: " + actionName + " for delivery offer " + offerId, ex);
        }
    }

    record DeliveryOfferCardView(
        Long id,
        String headline,
        String requesterName,
        String targetName,
        String bookPair,
        Double distanceKm,
        String status,
        String assignedDeliveryPartnerName
    ) {
    }

    record Coordinate(
        Double latitude,
        Double longitude
    ) {
    }
}
