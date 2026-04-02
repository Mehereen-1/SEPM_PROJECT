package com.example.project.admin.facade;

import com.example.project.admin.dto.AdminExchangeRequestResponse;
import com.example.project.admin.dto.AdminOfferResponse;
import com.example.project.admin.dto.AdminUserResponse;
import com.example.project.admin.dto.BookAdminRequest;
import com.example.project.admin.dto.BookAdminResponse;
import com.example.project.admin.service.AdminExchangeService;
import com.example.project.admin.service.IAdminBookService;
import com.example.project.admin.service.IAdminOfferService;
import com.example.project.admin.service.IAdminUserService;
import com.example.project.admin.strategy.AdminActionContext;
import com.example.project.admin.strategy.AdminActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminFacade {

    @Autowired
    private IAdminBookService adminBookService;

    @Autowired
    private IAdminUserService adminUserService;

    @Autowired
    private IAdminOfferService adminOfferService;

    @Autowired
    private AdminExchangeService adminExchangeService;

    @Autowired
    private AdminActionContext adminActionContext;

    public List<BookAdminResponse> getAllBooks() {
        return adminBookService.getAllBooks();
    }

    public ResponseEntity<?> createBook(BookAdminRequest request) {
        return adminBookService.createBook(request);
    }

    public ResponseEntity<?> updateBook(String id, BookAdminRequest request) {
        return adminBookService.updateBook(id, request);
    }

    public ResponseEntity<?> deleteBook(String id) {
        return adminBookService.deleteBook(id);
    }

    public List<AdminUserResponse> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    public ResponseEntity<?> blockUser(Long id) {
        adminActionContext.execute(AdminActionType.BLOCK_USER, id);

        return adminUserService.getAllUsers().stream()
            .filter(user -> user.id().equals(id))
            .findFirst()
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(Map.of("message", "User blocked successfully.")));
    }

    public List<AdminOfferResponse> getAllOffers() {
        return adminOfferService.getAllOffers();
    }

    public ResponseEntity<?> deleteOffer(Long id) {
        adminActionContext.execute(AdminActionType.DELETE_OFFER, id);
        return ResponseEntity.ok(Map.of("message", "Offer deleted successfully."));
    }

    public ResponseEntity<?> blockOffer(Long id) {
        return adminOfferService.blockOffer(id);
    }

    public ResponseEntity<?> approveExchange(Long id) {
        adminActionContext.execute(AdminActionType.APPROVE_EXCHANGE, id);
        return ResponseEntity.ok(Map.of("message", "Exchange request approved successfully."));
    }

    public List<AdminExchangeRequestResponse> getAllExchangeRequests() {
        return adminExchangeService.getAllExchangeRequests();
    }
}
