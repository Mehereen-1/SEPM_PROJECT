package com.example.project.notification.strategy;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.project.entity.Book;
import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.DeliveryPickupUser;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.Offer;
import com.example.project.entity.User;
import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;
import com.example.project.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryNotificationStrategy Unit Tests")
class DeliveryNotificationStrategyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeliveryNotificationStrategy strategy;

    @Test
    @DisplayName("Given first pickup approaching when building notifications then both readers receive role-specific updates")
    void givenFirstPickupApproaching_whenBuild_thenBothReadersReceiveRoleSpecificUpdates() {
        DeliveryOffer offer = deliveryOffer(101L, false, false);
        NotificationEvent event = new NotificationEvent(
            NotificationEventType.FIRST_PICKUP_APPROACHING,
            77L,
            null,
            offer,
            null,
            null,
            LocalDateTime.now()
        );

        List<NotificationDraft> drafts = strategy.build(event);

        assertEquals(2, drafts.size());
        assertEquals(2L, drafts.get(0).recipient().getId());
        assertTrue(drafts.get(0).message().contains("coming to pick up your book"));
        assertEquals("first-pickup-approaching:101:user:2", drafts.get(0).eventKey());

        assertEquals(3L, drafts.get(1).recipient().getId());
        assertTrue(drafts.get(1).message().contains("going to Reader A for first pickup"));
        assertEquals("first-pickup-approaching:101:user:3", drafts.get(1).eventKey());
    }

    @Test
    @DisplayName("Given first pickup completed when building PICKUP_STARTED notifications then both readers are notified")
    void givenFirstPickupCompleted_whenBuildPickupStarted_thenBothReadersAreNotified() {
        DeliveryOffer offer = deliveryOffer(102L, true, false);
        NotificationEvent event = new NotificationEvent(
            NotificationEventType.PICKUP_STARTED,
            77L,
            null,
            offer,
            null,
            null,
            LocalDateTime.now()
        );

        List<NotificationDraft> drafts = strategy.build(event);

        assertEquals(2, drafts.size());
        assertEquals(2L, drafts.get(0).recipient().getId());
        assertTrue(drafts.get(0).message().contains("has picked up your book"));
        assertEquals("first-pickup-completed:102:user:2", drafts.get(0).eventKey());

        assertEquals(3L, drafts.get(1).recipient().getId());
        assertTrue(drafts.get(1).message().contains("First pickup from Reader A is complete"));
        assertEquals("first-pickup-completed:102:user:3", drafts.get(1).eventKey());
    }

    @Test
    @DisplayName("Given second pickup completed when building BOOK_PICKED notifications then both readers are notified")
    void givenSecondPickupCompleted_whenBuildBookPicked_thenBothReadersAreNotified() {
        DeliveryOffer offer = deliveryOffer(103L, true, true);
        NotificationEvent event = new NotificationEvent(
            NotificationEventType.BOOK_PICKED,
            77L,
            null,
            offer,
            null,
            null,
            LocalDateTime.now()
        );

        List<NotificationDraft> drafts = strategy.build(event);

        assertEquals(2, drafts.size());
        assertEquals(3L, drafts.get(0).recipient().getId());
        assertTrue(drafts.get(0).message().contains("has delivered \"Requester Book\" to you"));
        assertEquals("second-pickup-completed:103:user:3", drafts.get(0).eventKey());

        assertEquals(2L, drafts.get(1).recipient().getId());
        assertTrue(drafts.get(1).message().contains("completed drop-off and pickup at Reader B's location"));
        assertEquals("second-pickup-completed:103:user:2", drafts.get(1).eventKey());
    }

    @Test
    @DisplayName("Given final delivery approaching when building notifications then both readers are notified")
    void givenFinalDeliveryApproaching_whenBuild_thenBothReadersAreNotified() {
        DeliveryOffer offer = deliveryOffer(104L, true, true);
        NotificationEvent event = new NotificationEvent(
            NotificationEventType.FINAL_DELIVERY_APPROACHING,
            77L,
            null,
            offer,
            null,
            null,
            LocalDateTime.now()
        );

        List<NotificationDraft> drafts = strategy.build(event);

        assertEquals(2, drafts.size());
        assertEquals(2L, drafts.get(0).recipient().getId());
        assertTrue(drafts.get(0).message().contains("is approaching you to deliver"));
        assertEquals("final-delivery-approaching:104:user:2", drafts.get(0).eventKey());

        assertEquals(3L, drafts.get(1).recipient().getId());
        assertTrue(drafts.get(1).message().contains("is on the way to deliver"));
        assertEquals("final-delivery-approaching:104:user:3", drafts.get(1).eventKey());
    }

    private DeliveryOffer deliveryOffer(Long id, boolean pickupACompleted, boolean pickupBCompleted) {
        User sender = new User();
        sender.setId(2L);
        sender.setName("Reader A");

        User receiver = new User();
        receiver.setId(3L);
        receiver.setName("Reader B");

        User assignee = new User();
        assignee.setId(5L);
        assignee.setName("Rider Hasan");

        Book senderBook = new Book();
        senderBook.setTitle("Requester Book");

        Book receiverBook = new Book();
        receiverBook.setTitle("Receiver Book");

        Offer requesterOffer = new Offer();
        requesterOffer.setUser(sender);
        requesterOffer.setBook(senderBook);

        Offer targetOffer = new Offer();
        targetOffer.setUser(receiver);
        targetOffer.setBook(receiverBook);

        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequesterOffer(requesterOffer);
        exchangeRequest.setTargetOffer(targetOffer);

        DeliveryOffer offer = new DeliveryOffer();
        offer.setId(id);
        offer.setExchangeRequest(exchangeRequest);
        offer.setAssignedDeliveryPartner(assignee);
        offer.setFirstPickupUser(DeliveryPickupUser.REQUESTER);
        offer.setPickupACompleted(pickupACompleted);
        offer.setPickupBCompleted(pickupBCompleted);
        return offer;
    }
}
