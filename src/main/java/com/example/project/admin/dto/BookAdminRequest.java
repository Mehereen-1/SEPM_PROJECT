package com.example.project.admin.dto;

public record BookAdminRequest(
    String title,
    String author,
    String isbn,
    String publisher,
    Integer publicationYear,
    String description,
    String coverImg
) {
    public BookAdminRequest(String title,
                            String author,
                            String isbn,
                            String publisher,
                            Integer publicationYear,
                            String description) {
        this(title, author, isbn, publisher, publicationYear, description, null);
    }
}
