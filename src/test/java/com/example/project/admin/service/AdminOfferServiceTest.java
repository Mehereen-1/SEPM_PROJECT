package com.example.project.admin.service;

import com.example.project.admin.dto.AdminOfferResponse;
import com.example.project.entity.Book;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.User;
import com.example.project.repository.OfferImageRepository;
import com.example.project.repository.OfferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private OfferImageRepository offerImageRepository;

    @InjectMocks
    private AdminOfferService adminOfferService;

    @Test
    @DisplayName("Given offers exist when getAllOffers then mapped response is returned")
    void givenOffersExist_whenGetAllOffers_thenReturnsMappedResponse() {
        Offer offer = buildOffer(10L, "Dune", "Alice", OfferStatus.ACTIVE, LocalDateTime.now());
        when(offerRepository.findAllWithDetails()).thenReturn(List.of(offer));

        List<AdminOfferResponse> result = adminOfferService.getAllOffers();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).offerId());
        assertEquals("Dune", result.get(0).bookTitle());
        assertEquals("Alice", result.get(0).ownerName());
    }

    @Test
    @DisplayName("Given missing offer when deleteOffer then not found")
    void givenMissingOffer_whenDeleteOffer_thenNotFound() {
        when(offerRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminOfferService.deleteOffer(404L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(offerImageRepository, never()).deleteByOffer_Id(404L);
    }

    @Test
    @DisplayName("Given existing offer when deleteOffer then images and offer are deleted")
    void givenExistingOffer_whenDeleteOffer_thenDeletesOfferAndImages() {
        Offer offer = buildOffer(11L, "1984", "Bob", OfferStatus.ACTIVE, LocalDateTime.now());
        when(offerRepository.findById(11L)).thenReturn(Optional.of(offer));

        ResponseEntity<?> response = adminOfferService.deleteOffer(11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(offerImageRepository).deleteByOffer_Id(11L);
        verify(offerRepository).delete(offer);
    }

    @Test
    @DisplayName("Given delete fails due to data references when deleteOffer then conflict")
    void givenDeleteFails_whenDeleteOffer_thenConflict() {
        Offer offer = buildOffer(12L, "Clean Code", "Carol", OfferStatus.ACTIVE, LocalDateTime.now());
        when(offerRepository.findById(12L)).thenReturn(Optional.of(offer));
        doThrow(new RuntimeException("fk violation")).when(offerRepository).delete(offer);

        ResponseEntity<?> response = adminOfferService.deleteOffer(12L);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("Given unknown offer when blockOffer then not found")
    void givenUnknownOffer_whenBlockOffer_thenNotFound() {
        when(offerRepository.findById(500L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminOfferService.blockOffer(500L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Given existing offer when blockOffer then status becomes blocked")
    void givenExistingOffer_whenBlockOffer_thenBlocksOffer() {
        Offer existing = buildOffer(13L, "Sapiens", "Dana", OfferStatus.ACTIVE, LocalDateTime.now());
        when(offerRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = adminOfferService.blockOffer(13L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(AdminOfferResponse.class, response.getBody());

        AdminOfferResponse body = (AdminOfferResponse) response.getBody();
        assertEquals("BLOCKED", body.status());
        assertTrue(existing.getStatus() == OfferStatus.BLOCKED);
    }

    private Offer buildOffer(Long id, String title, String owner, OfferStatus status, LocalDateTime createdAt) {
        Book book = new Book();
        book.setBookId("book-" + id);
        book.setTitle(title);
        book.setAuthor("Author");

        User user = new User();
        user.setId(id);
        user.setName(owner);
        user.setEmail(owner.toLowerCase() + "@example.com");
        user.setPassword("pw");

        Offer offer = new Offer();
        offer.setId(id);
        offer.setBook(book);
        offer.setUser(user);
        offer.setCondition("Good");
        offer.setStatus(status);
        offer.setCreatedAt(createdAt);
        return offer;
    }
}
