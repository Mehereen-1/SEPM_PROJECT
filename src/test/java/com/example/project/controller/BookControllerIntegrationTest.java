package com.example.project.controller;

import com.example.project.entity.Book;
import com.example.project.service.BookService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Book Controller Integration Tests")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("Given books exist when GET /books/browse then returns list")
    void givenBooksExist_whenBrowseBooks_thenReturnsList() throws Exception {
        Book one = new Book();
        one.setBookId("b1");
        one.setTitle("Dune");
        one.setAuthor("Frank Herbert");

        Book two = new Book();
        two.setBookId("b2");
        two.setTitle("1984");
        two.setAuthor("George Orwell");

        Page<Book> booksPage = new PageImpl<>(List.of(one, two), PageRequest.of(0, 20), 2);
        when(bookService.getAllBooksPaginated(any())).thenReturn(booksPage);

        mockMvc.perform(get("/books/browse?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].bookId").value("b1"))
            .andExpect(jsonPath("$.content[0].title").value("Dune"))
            .andExpect(jsonPath("$.content[1].bookId").value("b2"))
            .andExpect(jsonPath("$.content[1].author").value("George Orwell"))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    @DisplayName("Given existing book when GET /books/{id} then returns book")
    void givenExistingBook_whenGetById_thenReturnsBook() throws Exception {
        Book book = new Book();
        book.setBookId("book-10");
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");

        when(bookService.getBookById("book-10")).thenReturn(Optional.of(book));

        mockMvc.perform(get("/books/book-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value("book-10"))
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.author").value("Robert C. Martin"));
    }

    @Test
    @DisplayName("Given missing book when GET /books/{id} then returns 404")
    void givenMissingBook_whenGetById_thenReturnsNotFound() throws Exception {
        when(bookService.getBookById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/books/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given valid payload when POST /books by admin then returns created book")
    void givenValidPayload_whenAddBookAsAdmin_thenReturnsSavedBook() throws Exception {
        Book saved = new Book();
        saved.setBookId("book-100");
        saved.setTitle("The Pragmatic Programmer");
        saved.setAuthor("Andrew Hunt");
        saved.setLanguage("English");

        when(bookService.addBook(any(Book.class))).thenReturn(Optional.of(saved));

        String payload = """
            {
              "title": "The Pragmatic Programmer",
              "author": "Andrew Hunt",
              "language": "English",
              "isbn": "978-0201616224",
              "genres": "Programming"
            }
            """;

        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value("book-100"))
            .andExpect(jsonPath("$.title").value("The Pragmatic Programmer"));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookService).addBook(captor.capture());
        assertEquals("The Pragmatic Programmer", captor.getValue().getTitle());
        assertEquals("Andrew Hunt", captor.getValue().getAuthor());
        assertEquals("English", captor.getValue().getLanguage());
        assertNotNull(captor.getValue());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given duplicate title+author when POST /books then returns 409")
    void givenDuplicateBook_whenAddBook_thenReturnsConflict() throws Exception {
        when(bookService.addBook(any(Book.class))).thenReturn(Optional.empty());

        String payload = """
            {
              "title": "Dune",
              "author": "Frank Herbert"
            }
            """;

        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("A book with the same title and author already exists."));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given non-admin user when POST /books then returns forbidden")
    void givenNonAdmin_whenAddBook_thenForbidden() throws Exception {
        String payload = """
            {
              "title": "Dune",
              "author": "Frank Herbert"
            }
            """;

        assertThrows(ServletException.class, () -> mockMvc.perform(post("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)));
    }
}
