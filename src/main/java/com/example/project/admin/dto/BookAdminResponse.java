package com.example.project.admin.dto;

public record BookAdminResponse(
    String id,
    String title,
    String author,
    String isbn,
    String publisher,
    Integer publicationYear,
    String description,
    String coverImg
) {
    public BookAdminResponse(String id,
                             String title,
                             String author,
                             String isbn,
                             String publisher,
                             Integer publicationYear,
                             String description) {
        this(id, title, author, isbn, publisher, publicationYear, description, null);
    }
}
