package com.example.project.notification.strategy;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("Given first pickup completed when building BOOK_PICKED notification then receiver gets sender book update")
    void givenFirstPickupCompleted_whenBuildBookPicked_thenReceiverGetsSenderBookUpdate() {
        DeliveryOffer offer = deliveryOffer(101L, true, false);
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

        assertEquals(1, drafts.size());
        assertEquals(3L, drafts.get(0).recipient().getId());
        assertEquals("Book \"Requester Book\" has been picked and is on its way to you.", drafts.get(0).message());
        assertEquals("book-picked:101:receiver", drafts.get(0).eventKey());
    }

    @Test
    @DisplayName("Given both pickups completed when building BOOK_PICKED notification then sender gets receiver book update")
    void givenBothPickupsCompleted_whenBuildBookPicked_thenSenderGetsReceiverBookUpdate() {
        DeliveryOffer offer = deliveryOffer(102L, true, true);
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

        assertEquals(1, drafts.size());
        assertEquals(2L, drafts.get(0).recipient().getId());
        assertEquals("Book \"Receiver Book\" has been picked and is on its way to you.", drafts.get(0).message());
        assertEquals("book-picked:102:sender", drafts.get(0).eventKey());
    }

    private DeliveryOffer deliveryOffer(Long id, boolean pickupACompleted, boolean pickupBCompleted) {
        User sender = new User();
        sender.setId(2L);
        sender.setName("Reader A");

        User receiver = new User();
        receiver.setId(3L);
        receiver.setName("Reader B");

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
        offer.setFirstPickupUser(DeliveryPickupUser.REQUESTER);
        offer.setPickupACompleted(pickupACompleted);
        offer.setPickupBCompleted(pickupBCompleted);
        return offer;
    }
}
