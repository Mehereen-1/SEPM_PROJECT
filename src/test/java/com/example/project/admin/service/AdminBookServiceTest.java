package com.example.project.admin.service;

import com.example.project.admin.dto.BookAdminRequest;
import com.example.project.admin.dto.BookAdminResponse;
import com.example.project.entity.Book;
import com.example.project.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBookServiceTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private AdminBookService adminBookService;

    @Test
    @DisplayName("Given missing title when createBook then bad request")
    void givenMissingTitle_whenCreateBook_thenBadRequest() {
        BookAdminRequest request = new BookAdminRequest("   ", "Author", "123", "Pub", 2020, "Desc");

        ResponseEntity<?> response = adminBookService.createBook(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        verify(bookService, never()).addBook(any(Book.class));
    }

    @Test
    @DisplayName("Given valid create request when createBook then trimmed fields are saved and created returned")
    void givenValidRequest_whenCreateBook_thenCreated() {
        Book saved = new Book();
        saved.setBookId("book-1");
        saved.setTitle("Dune");
        saved.setAuthor("Frank Herbert");
        saved.setIsbn("978-1");
        saved.setPublisher("Ace");
        saved.setPublicationYear(1965);
        saved.setDescription("Classic");

        when(bookService.addBook(any(Book.class))).thenReturn(Optional.of(saved));

        BookAdminRequest request = new BookAdminRequest("  Dune ", " Frank Herbert  ", " 978-1 ", " Ace ", 1965, " Classic ");
        ResponseEntity<?> response = adminBookService.createBook(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertInstanceOf(BookAdminResponse.class, response.getBody());

        BookAdminResponse body = (BookAdminResponse) response.getBody();
        assertEquals("book-1", body.id());

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookService).addBook(captor.capture());
        assertEquals("Dune", captor.getValue().getTitle());
        assertEquals("Frank Herbert", captor.getValue().getAuthor());
        assertEquals("978-1", captor.getValue().getIsbn());
        assertEquals("Ace", captor.getValue().getPublisher());
    }

    @Test
    @DisplayName("Given duplicate book when createBook then conflict")
    void givenDuplicateBook_whenCreateBook_thenConflict() {
        when(bookService.addBook(any(Book.class))).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminBookService.createBook(
            new BookAdminRequest("Dune", "Frank Herbert", null, null, null, null)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("Given missing book when updateBook then not found")
    void givenMissingBook_whenUpdateBook_thenNotFound() {
        when(bookService.getBookById("missing-id")).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminBookService.updateBook(
            "missing-id",
            new BookAdminRequest("Dune", "Frank Herbert", null, null, null, null)
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(bookService, never()).saveBook(any(Book.class));
    }

    @Test
    @DisplayName("Given duplicate title and author for another book when updateBook then conflict")
    void givenDuplicateForAnotherBook_whenUpdateBook_thenConflict() {
        Book existing = new Book();
        existing.setBookId("book-1");
        existing.setTitle("Old");
        existing.setAuthor("Old Author");

        when(bookService.getBookById("book-1")).thenReturn(Optional.of(existing));
        when(bookService.isDuplicateBookForAnotherRecord("Dune", "Frank Herbert", "book-1")).thenReturn(true);

        ResponseEntity<?> response = adminBookService.updateBook(
            "book-1",
            new BookAdminRequest(" Dune ", " Frank Herbert ", null, null, null, null)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(bookService, never()).saveBook(any(Book.class));
    }

    @Test
    @DisplayName("Given valid update request when updateBook then saves and returns updated response")
    void givenValidUpdate_whenUpdateBook_thenReturnsUpdatedResponse() {
        Book existing = new Book();
        existing.setBookId("book-2");

        Book saved = new Book();
        saved.setBookId("book-2");
        saved.setTitle("New Title");
        saved.setAuthor("New Author");
        saved.setPublisher("Publisher");

        when(bookService.getBookById("book-2")).thenReturn(Optional.of(existing));
        when(bookService.isDuplicateBookForAnotherRecord("New Title", "New Author", "book-2")).thenReturn(false);
        when(bookService.saveBook(existing)).thenReturn(saved);

        ResponseEntity<?> response = adminBookService.updateBook(
            "book-2",
            new BookAdminRequest(" New Title ", " New Author ", null, " Publisher ", null, null)
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(BookAdminResponse.class, response.getBody());
        BookAdminResponse body = (BookAdminResponse) response.getBody();
        assertEquals("New Title", body.title());
        assertEquals("New Author", body.author());
    }

    @Test
    @DisplayName("Given delete for unknown id when deleteBook then not found")
    void givenUnknownId_whenDeleteBook_thenNotFound() {
        when(bookService.getBookById("missing")).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminBookService.deleteBook("missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Given existing id when deleteBook then success")
    void givenExistingId_whenDeleteBook_thenSuccess() {
        Book existing = new Book();
        existing.setBookId("book-3");
        when(bookService.getBookById("book-3")).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = adminBookService.deleteBook("book-3");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        verify(bookService).deleteBook("book-3");
    }
}
