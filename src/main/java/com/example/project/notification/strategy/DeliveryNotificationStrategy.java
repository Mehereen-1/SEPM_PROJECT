package com.example.project.notification.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryPickupUser;
import com.example.project.entity.User;
import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;
import com.example.project.notification.model.NotificationType;
import com.example.project.repository.UserRepository;

@Component
public class DeliveryNotificationStrategy implements NotificationStrategy {

    private static final List<String> DELIVERY_ROLE_NAMES = List.of(
        "DELIVERY_PARTNER", "DELIVERY", "ROLE_DELIVERY_PARTNER", "ROLE_DELIVERY"
    );

    private final UserRepository userRepository;

    public DeliveryNotificationStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supports(NotificationEventType eventType) {
        return eventType == NotificationEventType.DELIVERY_CREATED
            || eventType == NotificationEventType.DELIVERY_ASSIGNED
            || eventType == NotificationEventType.PICKUP_STARTED
            || eventType == NotificationEventType.BOOK_PICKED
            || eventType == NotificationEventType.DELIVERY_COMPLETED;
    }

    @Override
    public List<NotificationDraft> build(NotificationEvent event) {
        DeliveryOffer deliveryOffer = event.deliveryOffer();
        if (deliveryOffer == null || deliveryOffer.getExchangeRequest() == null) {
            return List.of();
        }

        User sender = safeSender(deliveryOffer);
        User receiver = safeReceiver(deliveryOffer);
        User assignee = deliveryOffer.getAssignedDeliveryPartner();

        Long deliveryOfferId = deliveryOffer.getId();
        List<NotificationDraft> drafts = new ArrayList<>();

        switch (event.eventType()) {
            case DELIVERY_CREATED -> {
                String senderBook = safeSenderBook(deliveryOffer);
                String receiverBook = safeReceiverBook(deliveryOffer);
                String message = "A new delivery task is available for exchange: \"" + senderBook + "\" <-> \"" + receiverBook + "\".";
                drafts.add(new NotificationDraft(sender, "Delivery offer created for your exchange request.", NotificationType.DELIVERY, "delivery-created:" + deliveryOfferId + ":sender"));
                drafts.add(new NotificationDraft(receiver, "Delivery offer created for your exchange request.", NotificationType.DELIVERY, "delivery-created:" + deliveryOfferId + ":receiver"));
                for (User deliveryPartner : findDeliveryPartners()) {
                    drafts.add(new NotificationDraft(deliveryPartner, message, NotificationType.DELIVERY, "delivery-created:" + deliveryOfferId + ":partner:" + deliveryPartner.getId()));
                }
            }
            case DELIVERY_ASSIGNED -> {
                String assigneeName = assignee != null ? assignee.getName() : "A delivery partner";
                drafts.add(new NotificationDraft(sender, assigneeName + " accepted your delivery request.", NotificationType.DELIVERY, "delivery-assigned:" + deliveryOfferId + ":sender"));
                drafts.add(new NotificationDraft(receiver, assigneeName + " accepted your delivery request.", NotificationType.DELIVERY, "delivery-assigned:" + deliveryOfferId + ":receiver"));
            }
            case PICKUP_STARTED -> {
                DeliveryPickupUser firstPickupUser = resolveFirstPickupUser(deliveryOffer);
                if (firstPickupUser == DeliveryPickupUser.RECEIVER) {
                    drafts.add(new NotificationDraft(
                        receiver,
                        "Delivery partner has picked up your book \"" + safeReceiverBook(deliveryOffer) + "\".",
                        NotificationType.DELIVERY,
                        "pickup-started:" + deliveryOfferId + ":receiver"
                    ));
                } else {
                    drafts.add(new NotificationDraft(
                        sender,
                        "Delivery partner has picked up your book \"" + safeSenderBook(deliveryOffer) + "\".",
                        NotificationType.DELIVERY,
                        "pickup-started:" + deliveryOfferId + ":sender"
                    ));
                }
            }
            case BOOK_PICKED -> {
                if (Boolean.TRUE.equals(deliveryOffer.getPickupACompleted()) && Boolean.TRUE.equals(deliveryOffer.getPickupBCompleted())) {
                    drafts.add(new NotificationDraft(
                        sender,
                        "Book \"" + safeReceiverBook(deliveryOffer) + "\" has been picked and is on its way to you.",
                        NotificationType.DELIVERY,
                        "book-picked:" + deliveryOfferId + ":sender"
                    ));
                } else {
                    drafts.add(new NotificationDraft(
                        receiver,
                        "Book \"" + safeSenderBook(deliveryOffer) + "\" has been picked and is on its way to you.",
                        NotificationType.DELIVERY,
                        "book-picked:" + deliveryOfferId + ":receiver"
                    ));
                }
            }
            case DELIVERY_COMPLETED -> {
                String senderBook = safeSenderBook(deliveryOffer);
                String receiverBook = safeReceiverBook(deliveryOffer);
                String completionMessage = "Delivery completed for exchange: \"" + senderBook + "\" <-> \"" + receiverBook + "\".";
                drafts.add(new NotificationDraft(sender, completionMessage, NotificationType.DELIVERY, "delivery-completed:" + deliveryOfferId + ":sender"));
                drafts.add(new NotificationDraft(receiver, completionMessage, NotificationType.DELIVERY, "delivery-completed:" + deliveryOfferId + ":receiver"));
                if (assignee != null) {
                    drafts.add(new NotificationDraft(assignee, "You completed the delivery successfully.", NotificationType.DELIVERY, "delivery-completed:" + deliveryOfferId + ":assignee"));
                }
            }
            default -> {
                return List.of();
            }
        }

        return deduplicateByRecipientAndEventKey(drafts);
    }

    private List<User> findDeliveryPartners() {
        return userRepository.findByAnyRoleNames(DELIVERY_ROLE_NAMES);
    }

    private List<NotificationDraft> deduplicateByRecipientAndEventKey(List<NotificationDraft> drafts) {
        Set<String> seen = new HashSet<>();
        List<NotificationDraft> unique = new ArrayList<>();
        for (NotificationDraft draft : drafts) {
            if (draft.recipient() == null || draft.recipient().getId() == null) {
                continue;
            }
            String key = draft.recipient().getId() + "::" + draft.eventKey();
            if (seen.add(key)) {
                unique.add(draft);
            }
        }
        return unique;
    }

    private User safeSender(DeliveryOffer deliveryOffer) {
        try {
            return deliveryOffer.getExchangeRequest().getRequesterOffer().getUser();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private User safeReceiver(DeliveryOffer deliveryOffer) {
        try {
            return deliveryOffer.getExchangeRequest().getTargetOffer().getUser();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String safeSenderBook(DeliveryOffer deliveryOffer) {
        try {
            String title = deliveryOffer.getExchangeRequest().getRequesterOffer().getBook().getTitle();
            return title != null && !title.isBlank() ? title : "your selected book";
        } catch (RuntimeException ex) {
            return "your selected book";
        }
    }

    private String safeReceiverBook(DeliveryOffer deliveryOffer) {
        try {
            String title = deliveryOffer.getExchangeRequest().getTargetOffer().getBook().getTitle();
            return title != null && !title.isBlank() ? title : "the requested book";
        } catch (RuntimeException ex) {
            return "the requested book";
        }
    }

    private DeliveryPickupUser resolveFirstPickupUser(DeliveryOffer deliveryOffer) {
        if (deliveryOffer == null || deliveryOffer.getFirstPickupUser() == null) {
            return DeliveryPickupUser.REQUESTER;
        }
        return deliveryOffer.getFirstPickupUser();
    }
}
