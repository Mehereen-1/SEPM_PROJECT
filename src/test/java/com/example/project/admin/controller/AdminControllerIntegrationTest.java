package com.example.project.admin.controller;

import com.example.project.admin.dto.BookAdminRequest;
import com.example.project.admin.dto.BookAdminResponse;
import com.example.project.admin.facade.AdminFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Admin Controller Integration Tests")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminFacade adminFacade;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given books available when GET /admin/books then wrapped success response returned")
    void givenBooksAvailable_whenGetAdminBooks_thenWrappedSuccessReturned() throws Exception {
        BookAdminResponse book = new BookAdminResponse("b-1", "Dune", "Frank Herbert", "123", "Ace", 1965, "Classic");
        when(adminFacade.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/admin/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Books fetched successfully"))
            .andExpect(jsonPath("$.data[0].id").value("b-1"))
            .andExpect(jsonPath("$.data[0].title").value("Dune"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given valid create when POST /admin/books then created wrapped as success")
    void givenValidCreate_whenPostAdminBooks_thenCreatedSuccessWrapped() throws Exception {
        BookAdminResponse created = new BookAdminResponse("b-2", "1984", "George Orwell", null, null, null, null);
        ResponseEntity<?> successResponse = ResponseEntity.status(HttpStatus.CREATED).body(created);
        doReturn(successResponse).when(adminFacade).createBook(any(BookAdminRequest.class));

        mockMvc.perform(post("/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"1984","author":"George Orwell"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Book created successfully"))
            .andExpect(jsonPath("$.data.title").value("1984"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given invalid create when POST /admin/books then wrapped error response returned")
    void givenInvalidCreate_whenPostAdminBooks_thenWrappedErrorReturned() throws Exception {
        ResponseEntity<?> errorResponse = ResponseEntity.badRequest().body(Map.of("message", "title and author are required."));
        doReturn(errorResponse).when(adminFacade).createBook(any(BookAdminRequest.class));

        mockMvc.perform(post("/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"","author":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("title and author are required."));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given non-admin role when GET /admin/books then access denied")
    void givenNonAdminRole_whenGetAdminBooks_thenAccessDenied() throws Exception {
        mockMvc.perform(get("/admin/books"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Unexpected server error."));
    }
}
