package com.example.project.admin.controller;

import com.example.project.admin.dto.AdminApiResponse;
import com.example.project.admin.dto.AdminExchangeRequestResponse;
import com.example.project.admin.dto.AdminOfferResponse;
import com.example.project.admin.dto.AdminUserResponse;
import com.example.project.admin.dto.BookAdminRequest;
import com.example.project.admin.dto.BookAdminResponse;
import com.example.project.admin.facade.AdminFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminFacade adminFacade;

    @GetMapping("/books")
    public ResponseEntity<AdminApiResponse<List<BookAdminResponse>>> getAllBooks() {
        return ResponseEntity.ok(AdminApiResponse.success("Books fetched successfully", adminFacade.getAllBooks()));
    }

    @PostMapping("/books")
    public ResponseEntity<AdminApiResponse<Object>> createBook(@RequestBody BookAdminRequest request) {
        return wrapFacadeResponse(adminFacade.createBook(request), "Book created successfully");
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<AdminApiResponse<Object>> updateBook(@PathVariable String id, @RequestBody BookAdminRequest request) {
        return wrapFacadeResponse(adminFacade.updateBook(id, request), "Book updated successfully");
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<AdminApiResponse<Object>> deleteBook(@PathVariable String id) {
        return wrapFacadeResponse(adminFacade.deleteBook(id), "Book deleted successfully");
    }

    @GetMapping("/users")
    public ResponseEntity<AdminApiResponse<List<AdminUserResponse>>> getAllUsers() {
        return ResponseEntity.ok(AdminApiResponse.success("Users fetched successfully", adminFacade.getAllUsers()));
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<AdminApiResponse<Object>> blockUser(@PathVariable Long id) {
        return wrapFacadeResponse(adminFacade.blockUser(id), "User blocked successfully");
    }

    @GetMapping("/offers")
    public ResponseEntity<AdminApiResponse<List<AdminOfferResponse>>> getAllOffers() {
        return ResponseEntity.ok(AdminApiResponse.success("Offers fetched successfully", adminFacade.getAllOffers()));
    }

    @DeleteMapping("/offers/{id}")
    public ResponseEntity<AdminApiResponse<Object>> deleteOffer(@PathVariable Long id) {
        return wrapFacadeResponse(adminFacade.deleteOffer(id), "Offer deleted successfully");
    }

    @PutMapping("/offers/{id}/block")
    public ResponseEntity<AdminApiResponse<Object>> blockOffer(@PathVariable Long id) {
        return wrapFacadeResponse(adminFacade.blockOffer(id), "Offer blocked successfully");
    }

    @GetMapping("/exchange-requests")
    public ResponseEntity<AdminApiResponse<List<AdminExchangeRequestResponse>>> getAllExchangeRequests() {
        return ResponseEntity.ok(AdminApiResponse.success("Exchange requests fetched successfully", adminFacade.getAllExchangeRequests()));
    }

    @PutMapping("/exchange-requests/{id}/approve")
    public ResponseEntity<AdminApiResponse<Object>> approveExchangeRequest(@PathVariable Long id) {
        return wrapFacadeResponse(adminFacade.approveExchange(id), "Exchange request approved successfully");
    }

    private ResponseEntity<AdminApiResponse<Object>> wrapFacadeResponse(ResponseEntity<?> source, String defaultSuccessMessage) {
        Object body = source.getBody();
        String message = extractMessage(body, source.getStatusCode().is2xxSuccessful() ? defaultSuccessMessage : "Request failed");

        if (source.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(source.getStatusCode())
                .body(AdminApiResponse.success(message, body));
        }

        return ResponseEntity.status(source.getStatusCode())
            .body(AdminApiResponse.error(message, body));
    }

    private String extractMessage(Object body, String fallback) {
        if (body instanceof Map<?, ?> map) {
            Object rawMessage = map.get("message");
            if (rawMessage != null) {
                return String.valueOf(rawMessage);
            }
        }
        return fallback;
    }
}
