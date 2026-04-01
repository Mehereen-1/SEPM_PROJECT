package com.example.project.service;

import com.example.project.entity.Book;
import com.example.project.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("Given books exist when getAllBooks then returns repository data")
    void givenBooksExist_whenGetAllBooks_thenReturnsRepositoryData() {
        Book one = new Book();
        one.setBookId("b1");
        one.setTitle("Book One");

        when(bookRepository.findAll()).thenReturn(List.of(one));

        List<Book> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getBookId());
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("Given duplicate title author when addBook then returns empty and does not save")
    void givenDuplicateBook_whenAddBook_thenReturnsEmpty() {
        Book candidate = new Book();
        candidate.setTitle("Dune");
        candidate.setAuthor("Frank Herbert");

        when(bookRepository.findByTitleIgnoreCaseAndAuthorIgnoreCase("Dune", "Frank Herbert"))
            .thenReturn(Optional.of(new Book()));

        Optional<Book> result = bookService.addBook(candidate);

        assertTrue(result.isEmpty());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Given unique title author when addBook then assigns id and saves")
    void givenUniqueBook_whenAddBook_thenAssignsIdAndSaves() {
        Book candidate = new Book();
        candidate.setTitle("Project Hail Mary");
        candidate.setAuthor("Andy Weir");

        when(bookRepository.findByTitleIgnoreCaseAndAuthorIgnoreCase("Project Hail Mary", "Andy Weir"))
            .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Book> result = bookService.addBook(candidate);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getBookId());

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertNotNull(captor.getValue().getBookId());
    }

    @Test
    @DisplayName("Given same title author with different id when duplicate check then returns repository value")
    void givenTitleAuthorWithDifferentId_whenDuplicateCheck_thenReturnsRepositoryValue() {
        when(bookRepository.existsByTitleIgnoreCaseAndAuthorIgnoreCaseAndBookIdNot("1984", "George Orwell", "b-10"))
            .thenReturn(true);

        boolean duplicate = bookService.isDuplicateBookForAnotherRecord("1984", "George Orwell", "b-10");

        assertTrue(duplicate);
        verify(bookRepository).existsByTitleIgnoreCaseAndAuthorIgnoreCaseAndBookIdNot("1984", "George Orwell", "b-10");
    }

    @Test
    @DisplayName("Given filters when search methods are called then delegates to repository")
    void givenFilters_whenSearchMethodsCalled_thenDelegatesToRepository() {
        when(bookRepository.findByTitleContainingIgnoreCase("dune")).thenReturn(List.of());
        when(bookRepository.findByAuthorContainingIgnoreCase("orwell")).thenReturn(List.of());
        when(bookRepository.findByLanguage("English")).thenReturn(List.of());
        when(bookRepository.findByGenresContaining("Sci-Fi")).thenReturn(List.of());

        assertTrue(bookService.searchByTitle("dune").isEmpty());
        assertTrue(bookService.searchByAuthor("orwell").isEmpty());
        assertTrue(bookService.getBooksByLanguage("English").isEmpty());
        assertTrue(bookService.searchByGenre("Sci-Fi").isEmpty());

        verify(bookRepository).findByTitleContainingIgnoreCase("dune");
        verify(bookRepository).findByAuthorContainingIgnoreCase("orwell");
        verify(bookRepository).findByLanguage("English");
        verify(bookRepository).findByGenresContaining("Sci-Fi");
    }

    @Test
    @DisplayName("Given id when deleteBook then deleteById called")
    void givenId_whenDeleteBook_thenDelegatesToRepository() {
        bookService.deleteBook("b-88");

        verify(bookRepository).deleteById("b-88");
    }

    @Test
    @DisplayName("Given repository count when getTotalBooks then returns count")
    void givenRepositoryCount_whenGetTotalBooks_thenReturnsCount() {
        when(bookRepository.count()).thenReturn(27L);

        long total = bookService.getTotalBooks();

        assertEquals(27L, total);
    }
}
