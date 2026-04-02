package com.example.project.admin.dto;

public record AdminUserResponse(
    Long id,
    String name,
    String email,
    String role,
    String status
) {
}
