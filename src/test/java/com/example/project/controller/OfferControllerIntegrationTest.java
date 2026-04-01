package com.example.project.controller;

import com.example.project.entity.Book;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferImage;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.User;
import com.example.project.repository.BookRepository;
import com.example.project.repository.OfferImageRepository;
import com.example.project.repository.OfferRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Offer Controller Integration Tests")
class OfferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfferRepository offerRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private OfferImageRepository offerImageRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityUtil securityUtil;

    @Test
    @DisplayName("Given active offers when GET /offers then returns mapped browse response")
    void givenActiveOffers_whenBrowse_thenReturnsMappedResponse() throws Exception {
        User owner = new User();
        owner.setId(5L);
        owner.setName("Offer Owner");
        owner.setLatitude(23.81);
        owner.setLongitude(90.41);
        owner.setAddress("Dhaka");

        User current = new User();
        current.setId(7L);
        current.setName("Current User");
        current.setLatitude(23.75);
        current.setLongitude(90.39);
        current.setAddress("Banani");

        Book book = new Book();
        book.setBookId("book-1");
        book.setTitle("Dune");
        book.setAuthor("Frank Herbert");

        Offer offer = new Offer();
        offer.setId(11L);
        offer.setUser(owner);
        offer.setBook(book);
        offer.setCondition("Good");
        offer.setNote("Well kept");
        offer.setStatus(OfferStatus.ACTIVE);
        offer.setCreatedAt(LocalDateTime.now());

        OfferImage image = new OfferImage();
        image.setOffer(offer);
        image.setImageUrl("/uploads/offers/11/a.jpg");

        when(offerRepository.findByStatusWithDetails(OfferStatus.ACTIVE)).thenReturn(List.of(offer));
        when(offerImageRepository.findByOfferIdIn(List.of(11L))).thenReturn(List.of(image));
        when(securityUtil.getCurrentUsername()).thenReturn("reader@example.com");
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(current));

        mockMvc.perform(get("/offers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].offerId").value(11))
            .andExpect(jsonPath("$[0].bookTitle").value("Dune"))
            .andExpect(jsonPath("$[0].ownerName").value("Offer Owner"))
            .andExpect(jsonPath("$[0].imageUrls[0]").value("/uploads/offers/11/a.jpg"))
            .andExpect(jsonPath("$[0].currentUserName").value("Current User"));
    }

    @Test
    @DisplayName("Given missing bookId when POST /offers then returns 400")
    void givenMissingBookId_whenCreateOffer_thenBadRequest() throws Exception {
        mockMvc.perform(post("/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"condition":"Good"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("bookId is required."));
    }

    @Test
    @DisplayName("Given unknown book when POST /offers then returns 404")
    void givenUnknownBook_whenCreateOffer_thenNotFound() throws Exception {
        when(bookRepository.findById("missing-book")).thenReturn(Optional.empty());

        mockMvc.perform(post("/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"bookId":"missing-book","condition":"Good"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Book not found."));
    }

    @Test
    @DisplayName("Given anonymous user when POST /offers then returns 401")
    void givenAnonymous_whenCreateOffer_thenUnauthorized() throws Exception {
        Book book = new Book();
        book.setBookId("book-1");
        book.setTitle("Dune");
        book.setAuthor("Frank Herbert");

        when(bookRepository.findById("book-1")).thenReturn(Optional.of(book));
        when(securityUtil.getCurrentUsername()).thenReturn("anonymousUser");

        mockMvc.perform(post("/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"bookId":"book-1","condition":"Good"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("User is not authenticated."));
    }

    @Test
    @DisplayName("Given valid request when POST /offers then returns created")
    void givenValidRequest_whenCreateOffer_thenCreated() throws Exception {
        Book book = new Book();
        book.setBookId("book-1");
        book.setTitle("Dune");
        book.setAuthor("Frank Herbert");

        User user = new User();
        user.setId(2L);
        user.setName("Reader");
        user.setEmail("reader@example.com");
        user.setPassword("pw");

        Offer saved = new Offer();
        saved.setId(99L);
        saved.setBook(book);
        saved.setUser(user);
        saved.setCondition("Like New");
        saved.setNote("First owner");
        saved.setStatus(OfferStatus.ACTIVE);
        saved.setCreatedAt(LocalDateTime.now());

        when(bookRepository.findById("book-1")).thenReturn(Optional.of(book));
        when(securityUtil.getCurrentUsername()).thenReturn("reader@example.com");
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(offerRepository.save(any(Offer.class))).thenReturn(saved);

        mockMvc.perform(post("/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bookId":"book-1",
                      "condition":"Like New",
                      "note":"First owner"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(99))
            .andExpect(jsonPath("$.bookId").value("book-1"))
            .andExpect(jsonPath("$.userId").value(2))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Given unauthenticated user when GET /offers/my-active then returns 401")
    void givenUnauthenticated_whenGetMyActive_thenUnauthorized() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("anonymousUser");

        mockMvc.perform(get("/offers/my-active"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("User is not authenticated."));
    }

    @Test
    @DisplayName("Given authenticated user when GET /offers/my-active then returns active offers")
    void givenAuthenticated_whenGetMyActive_thenReturnsOffers() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setName("Alice");
        user.setEmail("alice@example.com");

        Book book = new Book();
        book.setBookId("book-2");
        book.setTitle("1984");

        Offer offer = new Offer();
        offer.setId(55L);
        offer.setUser(user);
        offer.setBook(book);
        offer.setCondition("Good");
        offer.setStatus(OfferStatus.ACTIVE);
        offer.setCreatedAt(LocalDateTime.now());

        when(securityUtil.getCurrentUsername()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(offerRepository.findByUserIdAndStatusWithDetails(3L, OfferStatus.ACTIVE)).thenReturn(List.of(offer));

        mockMvc.perform(get("/offers/my-active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].offerId").value(55))
            .andExpect(jsonPath("$[0].bookTitle").value("1984"));
    }

    @Test
    @DisplayName("Given non-owner when PUT /offers/{id} then returns 403")
    void givenNonOwner_whenUpdateOffer_thenForbidden() throws Exception {
        User current = new User();
        current.setId(10L);
        current.setEmail("ownerA@example.com");

        User owner = new User();
        owner.setId(20L);

        Book book = new Book();
        book.setBookId("book-2");

        Offer offer = new Offer();
        offer.setId(5L);
        offer.setUser(owner);
        offer.setBook(book);
        offer.setCondition("Good");
        offer.setStatus(OfferStatus.ACTIVE);
        offer.setCreatedAt(LocalDateTime.now());

        when(securityUtil.getCurrentUsername()).thenReturn("ownerA@example.com");
        when(userRepository.findByEmail("ownerA@example.com")).thenReturn(Optional.of(current));
        when(offerRepository.findById(5L)).thenReturn(Optional.of(offer));

        mockMvc.perform(put("/offers/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"condition":"Fair","note":"Updated"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("You can only modify your own offers."));

        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    @DisplayName("Given owner when DELETE /offers/{id} then returns success")
    void givenOwner_whenDeleteOffer_thenSuccess() throws Exception {
        User current = new User();
        current.setId(30L);
        current.setEmail("owner@example.com");

        Book book = new Book();
        book.setBookId("book-3");

        Offer offer = new Offer();
        offer.setId(8L);
        offer.setUser(current);
        offer.setBook(book);
        offer.setCondition("Good");
        offer.setStatus(OfferStatus.ACTIVE);
        offer.setCreatedAt(LocalDateTime.now());

        when(securityUtil.getCurrentUsername()).thenReturn("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(current));
        when(offerRepository.findById(8L)).thenReturn(Optional.of(offer));

        mockMvc.perform(delete("/offers/8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Offer deleted successfully."));

        verify(offerImageRepository).deleteByOffer_Id(8L);
        verify(offerRepository).delete(offer);
    }
}
