package com.example.project.admin.strategy;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AdminActionContext {

    private final Map<AdminActionType, AdminActionStrategy> strategies = new EnumMap<>(AdminActionType.class);

    public AdminActionContext(
        BlockUserStrategy blockUserStrategy,
        DeleteOfferStrategy deleteOfferStrategy,
        ApproveExchangeStrategy approveExchangeStrategy
    ) {
        strategies.put(AdminActionType.BLOCK_USER, blockUserStrategy);
        strategies.put(AdminActionType.DELETE_OFFER, deleteOfferStrategy);
        strategies.put(AdminActionType.APPROVE_EXCHANGE, approveExchangeStrategy);
    }

    public void execute(AdminActionType actionType, Long id) {
        AdminActionStrategy strategy = strategies.get(actionType);
        if (strategy == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported admin action: " + actionType);
        }
        strategy.execute(id);
    }
}
