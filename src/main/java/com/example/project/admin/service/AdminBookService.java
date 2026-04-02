package com.example.project.admin.service;

import com.example.project.admin.dto.BookAdminRequest;
import com.example.project.admin.dto.BookAdminResponse;
import com.example.project.entity.Book;
import com.example.project.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminBookService implements IAdminBookService {

    @Autowired
    private BookService bookService;

    public List<BookAdminResponse> getAllBooks() {
        return bookService.getAllBooks()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ResponseEntity<?> createBook(BookAdminRequest request) {
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
        book.setCoverImg(normalize(request.coverImg()));

        return bookService.addBook(book)
            .<ResponseEntity<?>>map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "A book with the same title and author already exists.")));
    }

    public ResponseEntity<?> updateBook(String id, BookAdminRequest request) {
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
        existing.setCoverImg(normalize(request.coverImg()));

        Book saved = bookService.saveBook(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    public ResponseEntity<?> deleteBook(String id) {
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
            book.getDescription(),
            book.getCoverImg()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
