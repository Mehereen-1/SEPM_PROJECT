package com.example.project.admin.controller;

import com.example.project.admin.dto.AdminApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages = "com.example.project.admin.controller")
public class AdminExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<AdminApiResponse<Object>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Request failed.";
        return ResponseEntity.status(ex.getStatusCode())
            .body(AdminApiResponse.error(message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AdminApiResponse<Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(AdminApiResponse.error("Unexpected server error.", null));
    }
}
