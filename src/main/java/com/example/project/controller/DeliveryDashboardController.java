package com.example.project.controller;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryOfferStatus;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.User;
import com.example.project.repository.DeliveryOfferRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class DeliveryDashboardController {

    @Autowired
    private DeliveryOfferRepository deliveryOfferRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/delivery/dashboard")
    public String deliveryDashboard(Model model) {
        ensureAvailableOffersAreGenerated();
        List<DeliveryOfferCardView> cards = buildCards(deliveryOfferRepository.findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE));
        populateModel(model, cards, "Delivery Dashboard", "Book Exchange Offers", "Browse and accept exchange deliveries from readers.", true, false, false);
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/offers")
    public String offers(Model model) {
        ensureAvailableOffersAreGenerated();
        List<DeliveryOfferCardView> cards = buildCards(deliveryOfferRepository.findByStatusWithDetails(DeliveryOfferStatus.AVAILABLE));
        populateModel(model, cards, "Available Offers", "Book Exchange Offers", "Live exchange tasks waiting for a delivery partner.", true, false, false);
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
            .map(user -> deliveryOfferRepository.findByAssigneeAndStatusWithDetails(user.getId(), DeliveryOfferStatus.PENDING))
            .orElseGet(List::of)
            .stream()
            .map(this::toCard)
            .toList();

        populateModel(model, cards, "Pending Deliveries", "Pending Delivery Offers", "Deliveries you accepted and are currently handling.", false, true, false);
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

        populateModel(model, cards, "Completed Deliveries", "Completed Delivery Offers", "Completed exchange deliveries assigned to you.", false, false, true);
        return "delivery-dashboard";
    }

    @PostMapping("/delivery/accept/{id}")
    public String acceptOffer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
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
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setAcceptedAt(LocalDateTime.now());
        deliveryOfferRepository.save(offer);

        redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer accepted successfully.");
        return "redirect:/delivery/pending";
    }

    @PostMapping("/delivery/complete/{id}")
    public String completeOffer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("deliveryMessage", "You must be logged in to complete delivery offers.");
            return "redirect:/login";
        }

        Optional<DeliveryOffer> offerResult = deliveryOfferRepository.findByIdWithDetails(id);
        if (offerResult.isEmpty()) {
            redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer not found.");
            return "redirect:/delivery/pending";
        }

        DeliveryOffer offer = offerResult.get();
        if (offer.getStatus() != DeliveryOfferStatus.PENDING || offer.getAssignedDeliveryPartner() == null
            || !offer.getAssignedDeliveryPartner().getId().equals(currentUser.get().getId())) {
            redirectAttributes.addFlashAttribute("deliveryMessage", "You can only complete your own pending delivery offers.");
            return "redirect:/delivery/pending";
        }

        offer.setStatus(DeliveryOfferStatus.COMPLETED);
        offer.setCompletedAt(LocalDateTime.now());
        deliveryOfferRepository.save(offer);

        redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer marked as completed.");
        return "redirect:/delivery/completed";
    }

    @GetMapping("/delivery/location/{id}")
    public String findLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (deliveryOfferRepository.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("deliveryMessage", "Delivery offer not found.");
            return "redirect:/delivery/offers";
        }

        redirectAttributes.addFlashAttribute(
            "deliveryMessage",
            "Location tracking is not available yet for this task. This will be connected to map coordinates in the next iteration."
        );
        return "redirect:/delivery/offers";
    }

    private void populateModel(
        Model model,
        List<DeliveryOfferCardView> cards,
        String breadcrumbTitle,
        String pageHeading,
        String pageDescription,
        boolean showAcceptButton,
        boolean showCompleteButton,
        boolean completedView
    ) {
        model.addAttribute("offers", cards);
        model.addAttribute("breadcrumbTitle", breadcrumbTitle);
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("showAcceptButton", showAcceptButton);
        model.addAttribute("showCompleteButton", showCompleteButton);
        model.addAttribute("completedView", completedView);
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

        return new DeliveryOfferCardView(
            offer.getId(),
            headline,
            requesterName,
            targetName,
            bookPair,
            offer.getDistanceKm(),
            offer.getStatus().name(),
            offer.getAssignedDeliveryPartner() != null ? offer.getAssignedDeliveryPartner().getName() : null
        );
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
            deliveryOfferRepository.save(offer);
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
}
