package com.example.project.controller;

import com.example.project.entity.Book;
import com.example.project.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    /**
     * REST endpoint to browse all books
     * GET /books/browse
     * Returns a list of all books from the database
     */
    @GetMapping("/browse")
    public ResponseEntity<List<Book>> browseBooks() {
        List<Book> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }
    
    /**
     * Get book by ID
     * GET /books/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Add a new book to the catalog.
     * POST /books
     * Returns 409 if a book with the same title and author already exists.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addBook(@RequestBody BookRequest request) {
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setSeries(request.series());
        book.setRating(request.rating());
        book.setDescription(request.description());
        book.setLanguage(request.language());
        book.setIsbn(request.isbn());
        book.setGenres(request.genres());
        book.setCharacters(request.characters());
        book.setPages(request.pages());
        book.setFirstPublishDate(request.firstPublishDate());
        book.setAwards(request.awards());
        book.setCoverImg(request.coverImg());

        return bookService.addBook(book)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "A book with the same title and author already exists.")));
    }

    record BookRequest(
            String title,
            String author,
            String series,
            Double rating,
            String description,
            String language,
            String isbn,
            String genres,
            String characters,
            Integer pages,
            String firstPublishDate,
            String awards,
            String coverImg
    ) {}
}
