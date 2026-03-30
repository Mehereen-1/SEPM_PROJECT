package com.example.project.admin.dto;

public record BookAdminRequest(
    String title,
    String author,
    String isbn,
    String publisher,
    Integer publicationYear,
    String description
) {
}
