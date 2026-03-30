package com.example.project.repository;

import com.example.project.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    
    List<Book> findByTitleContainingIgnoreCase(String title);
    
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    List<Book> findByLanguage(String language);
    
    List<Book> findByGenresContaining(String genre);
    
    Optional<Book> findByBookId(String bookId);

    Optional<Book> findByTitleIgnoreCaseAndAuthorIgnoreCase(String title, String author);

    boolean existsByTitleIgnoreCaseAndAuthorIgnoreCaseAndBookIdNot(String title, String author, String bookId);
}

