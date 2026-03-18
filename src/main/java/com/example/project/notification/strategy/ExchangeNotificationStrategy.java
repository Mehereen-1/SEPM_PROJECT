package com.example.project.notification.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.project.entity.ExchangeRequest;
import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;
import com.example.project.notification.model.NotificationType;

@Component
public class ExchangeNotificationStrategy implements NotificationStrategy {

    @Override
    public boolean supports(NotificationEventType eventType) {
        return eventType == NotificationEventType.EXCHANGE_REQUEST_SENT
            || eventType == NotificationEventType.EXCHANGE_ACCEPTED
            || eventType == NotificationEventType.EXCHANGE_REJECTED;
    }

    @Override
    public List<NotificationDraft> build(NotificationEvent event) {
        ExchangeRequest exchangeRequest = event.exchangeRequest();
        if (exchangeRequest == null) {
            return List.of();
        }

        String requesterName = exchangeRequest.getRequesterOffer().getUser().getName();
        String targetName = exchangeRequest.getTargetOffer().getUser().getName();
        String requesterBook = exchangeRequest.getRequesterOffer().getBook().getTitle();
        String targetBook = exchangeRequest.getTargetOffer().getBook().getTitle();
        Long exchangeRequestId = exchangeRequest.getId();

        return switch (event.eventType()) {
            case EXCHANGE_REQUEST_SENT -> List.of(
                new NotificationDraft(
                    exchangeRequest.getTargetOffer().getUser(),
                    requesterName + " sent an exchange request: \"" + requesterBook + "\" for your \"" + targetBook + "\".",
                    NotificationType.EXCHANGE,
                    "exchange-request-sent:" + exchangeRequestId
                )
            );
            case EXCHANGE_ACCEPTED -> List.of(
                new NotificationDraft(
                    exchangeRequest.getRequesterOffer().getUser(),
                    targetName + " accepted your exchange request for \"" + targetBook + "\".",
                    NotificationType.EXCHANGE,
                    "exchange-accepted:" + exchangeRequestId
                )
            );
            case EXCHANGE_REJECTED -> List.of(
                new NotificationDraft(
                    exchangeRequest.getRequesterOffer().getUser(),
                    targetName + " rejected your exchange request for \"" + targetBook + "\".",
                    NotificationType.EXCHANGE,
                    "exchange-rejected:" + exchangeRequestId
                )
            );
            default -> List.of();
        };
    }
}
