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
            || eventType == NotificationEventType.FIRST_PICKUP_APPROACHING
            || eventType == NotificationEventType.PICKUP_STARTED
            || eventType == NotificationEventType.SECOND_PICKUP_APPROACHING
            || eventType == NotificationEventType.BOOK_PICKED
            || eventType == NotificationEventType.FINAL_DELIVERY_APPROACHING
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
        String assigneeName = safeUserName(assignee, "The delivery partner");
        PickupContext pickupContext = resolvePickupContext(deliveryOffer, sender, receiver);

        Long deliveryOfferId = deliveryOffer.getId();
        List<NotificationDraft> drafts = new ArrayList<>();

        switch (event.eventType()) {
            case DELIVERY_CREATED -> {
                String senderBook = safeSenderBook(deliveryOffer);
                String receiverBook = safeReceiverBook(deliveryOffer);
                String message = "A new delivery task is available for exchange: \"" + senderBook + "\" <-> \"" + receiverBook + "\".";
                drafts.add(new NotificationDraft(
                    sender,
                    "Delivery offer created for your exchange request.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("delivery-created", deliveryOfferId, sender)
                ));
                drafts.add(new NotificationDraft(
                    receiver,
                    "Delivery offer created for your exchange request.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("delivery-created", deliveryOfferId, receiver)
                ));
                for (User deliveryPartner : findDeliveryPartners()) {
                    drafts.add(new NotificationDraft(
                        deliveryPartner,
                        message,
                        NotificationType.DELIVERY,
                        eventKeyForRecipient("delivery-created-partner", deliveryOfferId, deliveryPartner)
                    ));
                }
            }
            case DELIVERY_ASSIGNED -> {
                drafts.add(new NotificationDraft(
                    sender,
                    assigneeName + " accepted your delivery request.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("delivery-assigned", deliveryOfferId, sender)
                ));
                drafts.add(new NotificationDraft(
                    receiver,
                    assigneeName + " accepted your delivery request.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("delivery-assigned", deliveryOfferId, receiver)
                ));
            }
            case FIRST_PICKUP_APPROACHING -> {
                drafts.add(new NotificationDraft(
                    pickupContext.firstReader(),
                    assigneeName + " is coming to pick up your book \"" + pickupContext.firstReaderBook() + "\" first.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("first-pickup-approaching", deliveryOfferId, pickupContext.firstReader())
                ));
                drafts.add(new NotificationDraft(
                    pickupContext.secondReader(),
                    assigneeName + " is going to " + pickupContext.firstReaderName()
                        + " for first pickup, then will collect your book.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("first-pickup-approaching", deliveryOfferId, pickupContext.secondReader())
                ));
            }
            case PICKUP_STARTED -> {
                drafts.add(new NotificationDraft(
                    pickupContext.firstReader(),
                    assigneeName + " has picked up your book \"" + pickupContext.firstReaderBook() + "\".",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("first-pickup-completed", deliveryOfferId, pickupContext.firstReader())
                ));
                drafts.add(new NotificationDraft(
                    pickupContext.secondReader(),
                    "First pickup from " + pickupContext.firstReaderName()
                        + " is complete. " + assigneeName + " will now head to you.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("first-pickup-completed", deliveryOfferId, pickupContext.secondReader())
                ));
            }
            case SECOND_PICKUP_APPROACHING -> {
                drafts.add(new NotificationDraft(
                    pickupContext.secondReader(),
                    assigneeName + " is coming to your location to deliver \"" + pickupContext.firstReaderBook()
                        + "\" and pick up your book \"" + pickupContext.secondReaderBook() + "\".",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("second-pickup-approaching", deliveryOfferId, pickupContext.secondReader())
                ));
                drafts.add(new NotificationDraft(
                    pickupContext.firstReader(),
                    assigneeName + " is now going to " + pickupContext.secondReaderName()
                        + " for drop-off and second pickup.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("second-pickup-approaching", deliveryOfferId, pickupContext.firstReader())
                ));
            }
            case BOOK_PICKED -> {
                drafts.add(new NotificationDraft(
                    pickupContext.secondReader(),
                    assigneeName + " has delivered \"" + pickupContext.firstReaderBook()
                        + "\" to you and picked up your book \"" + pickupContext.secondReaderBook() + "\".",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("second-pickup-completed", deliveryOfferId, pickupContext.secondReader())
                ));
                drafts.add(new NotificationDraft(
                    pickupContext.firstReader(),
                    assigneeName + " has completed drop-off and pickup at " + pickupContext.secondReaderName() + "'s location.",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("second-pickup-completed", deliveryOfferId, pickupContext.firstReader())
                ));
            }
            case FINAL_DELIVERY_APPROACHING -> {
                drafts.add(new NotificationDraft(
                    pickupContext.firstReader(),
                    assigneeName + " is approaching you to deliver \"" + pickupContext.secondReaderBook() + "\".",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("final-delivery-approaching", deliveryOfferId, pickupContext.firstReader())
                ));
                drafts.add(new NotificationDraft(
                    pickupContext.secondReader(),
                    assigneeName + " is on the way to deliver \"" + pickupContext.secondReaderBook()
                        + "\" to " + pickupContext.firstReaderName() + ".",
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("final-delivery-approaching", deliveryOfferId, pickupContext.secondReader())
                ));
            }
            case DELIVERY_COMPLETED -> {
                String senderBook = safeSenderBook(deliveryOffer);
                String receiverBook = safeReceiverBook(deliveryOffer);
                String completionMessage = "Delivery completed for exchange: \"" + senderBook + "\" <-> \"" + receiverBook + "\".";
                drafts.add(new NotificationDraft(
                    sender,
                    completionMessage,
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("delivery-completed", deliveryOfferId, sender)
                ));
                drafts.add(new NotificationDraft(
                    receiver,
                    completionMessage,
                    NotificationType.DELIVERY,
                    eventKeyForRecipient("delivery-completed", deliveryOfferId, receiver)
                ));
                if (assignee != null) {
                    drafts.add(new NotificationDraft(
                        assignee,
                        "You completed the delivery successfully.",
                        NotificationType.DELIVERY,
                        eventKeyForRecipient("delivery-completed-assignee", deliveryOfferId, assignee)
                    ));
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

    private PickupContext resolvePickupContext(DeliveryOffer deliveryOffer, User sender, User receiver) {
        if (resolveFirstPickupUser(deliveryOffer) == DeliveryPickupUser.RECEIVER) {
            return new PickupContext(
                receiver,
                sender,
                safeUserName(receiver, "Reader B"),
                safeUserName(sender, "Reader A"),
                safeReceiverBook(deliveryOffer),
                safeSenderBook(deliveryOffer)
            );
        }
        return new PickupContext(
            sender,
            receiver,
            safeUserName(sender, "Reader A"),
            safeUserName(receiver, "Reader B"),
            safeSenderBook(deliveryOffer),
            safeReceiverBook(deliveryOffer)
        );
    }

    private String safeUserName(User user, String fallback) {
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            return fallback;
        }
        return user.getName();
    }

    private String eventKeyForRecipient(String eventPrefix, Long deliveryOfferId, User recipient) {
        if (eventPrefix == null || deliveryOfferId == null || recipient == null || recipient.getId() == null) {
            return null;
        }
        return eventPrefix + ":" + deliveryOfferId + ":user:" + recipient.getId();
    }

    private record PickupContext(
        User firstReader,
        User secondReader,
        String firstReaderName,
        String secondReaderName,
        String firstReaderBook,
        String secondReaderBook
    ) {
    }
}
