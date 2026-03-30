package com.example.project.admin.service;

import com.example.project.admin.dto.BookAdminRequest;
import com.example.project.admin.dto.BookAdminResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IAdminBookService {
    List<BookAdminResponse> getAllBooks();

    ResponseEntity<?> createBook(BookAdminRequest request);

    ResponseEntity<?> updateBook(String id, BookAdminRequest request);

    ResponseEntity<?> deleteBook(String id);
}
