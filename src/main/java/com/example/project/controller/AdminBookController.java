package com.example.project.controller;

import com.example.project.entity.Book;
import com.example.project.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.Optional;

@RestController
@RequestMapping("/admin/books")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookController {

    @Autowired
    private BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookAdminResponse>> getAllBooks() {
        List<BookAdminResponse> books = bookService.getAllBooks()
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(books);
    }

    @PostMapping
    public ResponseEntity<?> createBook(@RequestBody BookAdminRequest request) {
        String title = normalize(request.title());
        String author = normalize(request.author());

        if (title == null || author == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "title and author are required."));
        }

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(normalize(request.isbn()));
        book.setPublisher(normalize(request.publisher()));
        book.setPublicationYear(request.publicationYear());
        book.setDescription(normalize(request.description()));

        return bookService.addBook(book)
            .<ResponseEntity<?>>map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "A book with the same title and author already exists.")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable String id, @RequestBody BookAdminRequest request) {
        Optional<Book> existingResult = bookService.getBookById(id);
        if (existingResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Book not found."));
        }

        String title = normalize(request.title());
        String author = normalize(request.author());

        if (title == null || author == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "title and author are required."));
        }

        if (bookService.isDuplicateBookForAnotherRecord(title, author, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "A book with the same title and author already exists."));
        }

        Book existing = existingResult.get();
        existing.setTitle(title);
        existing.setAuthor(author);
        existing.setIsbn(normalize(request.isbn()));
        existing.setPublisher(normalize(request.publisher()));
        existing.setPublicationYear(request.publicationYear());
        existing.setDescription(normalize(request.description()));

        Book saved = bookService.saveBook(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        Optional<Book> existingResult = bookService.getBookById(id);
        if (existingResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Book not found."));
        }

        bookService.deleteBook(id);
        return ResponseEntity.ok(Map.of("message", "Book deleted successfully."));
    }

    private BookAdminResponse toResponse(Book book) {
        return new BookAdminResponse(
            book.getBookId(),
            book.getTitle(),
            book.getAuthor(),
            book.getIsbn(),
            book.getPublisher(),
            book.getPublicationYear(),
            book.getDescription()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    record BookAdminRequest(
        String title,
        String author,
        String isbn,
        String publisher,
        Integer publicationYear,
        String description
    ) {
    }

    record BookAdminResponse(
        String id,
        String title,
        String author,
        String isbn,
        String publisher,
        Integer publicationYear,
        String description
    ) {
    }
}
