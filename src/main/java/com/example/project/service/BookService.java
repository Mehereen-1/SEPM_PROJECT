package com.example.project.service;

import com.example.project.entity.Book;
import com.example.project.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    /**
     * Get all books from database
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    /**
     * Get book by ID (bookId)
     */
    public Optional<Book> getBookById(String id) {
        return bookRepository.findById(id);
    }
    
    /**
     * Save a book to database
     */
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    /**
     * Add a new book after checking for duplicates by title and author.
     * Returns empty Optional if a book with the same title and author already exists.
     */
    public Optional<Book> addBook(Book book) {
        boolean exists = bookRepository
                .findByTitleIgnoreCaseAndAuthorIgnoreCase(book.getTitle(), book.getAuthor())
                .isPresent();
        if (exists) {
            return Optional.empty();
        }
        book.setBookId(UUID.randomUUID().toString());
        return Optional.of(bookRepository.save(book));
    }
    
    /**
     * Delete a book by ID
     */
    public void deleteBook(String id) {
        bookRepository.deleteById(id);
    }
    
    /**
     * Search books by title
     */
    public List<Book> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }
    
    /**
     * Search books by author
     */
    public List<Book> searchByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }
    
    /**
     * Get books by language
     */
    public List<Book> getBooksByLanguage(String language) {
        return bookRepository.findByLanguage(language);
    }
    
    /**
     * Search books by genre
     */
    public List<Book> searchByGenre(String genre) {
        return bookRepository.findByGenresContaining(genre);
    }
    
    /**
     * Get total book count
     */
    public long getTotalBooks() {
        return bookRepository.count();
    }
}

