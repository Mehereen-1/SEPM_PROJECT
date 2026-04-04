package com.example.project.controller;

import com.example.project.entity.Book;
import com.example.project.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * REST endpoint to browse all books with pagination
     * GET /books/browse?page=0&size=20
     * Returns a paginated list of books from the database
     */
    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browseBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Validate and set limits to prevent abuse
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 100) size = 100; // Max 100 items per page
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> booksPage = bookService.getAllBooksPaginated(pageable);
        
        return ResponseEntity.ok(Map.of(
            "content", booksPage.getContent(),
            "currentPage", booksPage.getNumber(),
            "totalItems", booksPage.getTotalElements(),
            "totalPages", booksPage.getTotalPages(),
            "pageSize", booksPage.getSize(),
            "hasNext", booksPage.hasNext(),
            "hasPrevious", booksPage.hasPrevious()
        ));
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
