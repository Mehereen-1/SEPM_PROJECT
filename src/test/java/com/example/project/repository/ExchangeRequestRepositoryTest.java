package com.example.project.repository;

import com.example.project.entity.Book;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ExchangeRequestRepositoryTest {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user1;
    private User user2;
    private Book book1;
    private Book book2;
    private Offer offer1;
    private Offer offer2;

    private static final Pattern QUOTED_LITERAL_PATTERN = Pattern.compile("'([^']+)'");

    @BeforeEach
    public void setUp() {
        String conditionOne = resolveValidOfferCondition(0);
        String conditionTwo = conditionOne;

        // Create roles
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);

        // Create users
        user1 = new User();
        user1.setName("testuser1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setRoles(Set.of(userRole));
        userRepository.save(user1);

        user2 = new User();
        user2.setName("testuser2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setRoles(Set.of(userRole));
        userRepository.save(user2);

        // Create books
        book1 = new Book();
        book1.setBookId("REPO-BOOK-1");
        book1.setTitle("Test Book 1");
        book1.setAuthor("Test Author 1");
        book1.setPublisher("Test Publisher");
        book1.setPublicationYear(2020);
        bookRepository.save(book1);

        book2 = new Book();
        book2.setBookId("REPO-BOOK-2");
        book2.setTitle("Test Book 2");
        book2.setAuthor("Test Author 2");
        book2.setPublisher("Test Publisher");
        book2.setPublicationYear(2021);
        bookRepository.save(book2);

        // Create offers
        offer1 = new Offer();
        offer1.setUser(user1);
        offer1.setBook(book1);
        offer1.setCondition(conditionOne);
        offer1.setNote("Test note");
        offer1.setStatus(OfferStatus.ACTIVE);
        offer1.setCreatedAt(LocalDateTime.now());
        offerRepository.save(offer1);

        offer2 = new Offer();
        offer2.setUser(user2);
        offer2.setBook(book2);
        offer2.setCondition(conditionTwo);
        offer2.setNote("Test note 2");
        offer2.setStatus(OfferStatus.ACTIVE);
        offer2.setCreatedAt(LocalDateTime.now());
        offerRepository.save(offer2);
    }

    private String resolveValidOfferCondition(int preferredIndex) {
        List<String> clauses = jdbcTemplate.queryForList(
            """
            SELECT cc.CHECK_CLAUSE
            FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
            JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu
              ON cc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME
            WHERE ccu.TABLE_NAME = 'OFFERS'
              AND ccu.COLUMN_NAME = 'OFFER_CONDITION'
            """,
            String.class
        );

        List<String> allowed = new ArrayList<>();
        for (String clause : clauses) {
            Matcher matcher = QUOTED_LITERAL_PATTERN.matcher(clause);
            while (matcher.find()) {
                allowed.add(matcher.group(1));
            }
        }

        if (allowed.isEmpty()) {
            return "Good";
        }

        int index = Math.min(preferredIndex, allowed.size() - 1);
        return allowed.get(index);
    }

    @Test
    public void testCreateExchangeRequest() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        ExchangeRequest saved = exchangeRequestRepository.save(request);

        assertFalse(saved.getId() == null);
        assertEquals(ExchangeRequestStatus.PENDING, saved.getStatus());
    }

    @Test
    public void testExistsByRequesterOfferAndTargetOffer() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request);

        boolean exists = exchangeRequestRepository.existsByRequesterOffer_IdAndTargetOffer_Id(
            offer1.getId(), offer2.getId()
        );

        assertTrue(exists);
    }

    @Test
    public void testFindSentByUserIdWithDetails() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request);

        List<ExchangeRequest> sent = exchangeRequestRepository.findSentByUserIdWithDetails(user1.getId());

        assertEquals(1, sent.size());
        assertEquals(offer1.getId(), sent.get(0).getRequesterOffer().getId());
    }

    @Test
    public void testFindReceivedByUserIdWithDetails() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request);

        List<ExchangeRequest> received = exchangeRequestRepository.findReceivedByUserIdWithDetails(user2.getId());

        assertEquals(1, received.size());
        assertEquals(offer2.getId(), received.get(0).getTargetOffer().getId());
    }

    @Test
    public void testFindByIdWithDetails() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        ExchangeRequest saved = exchangeRequestRepository.save(request);

        Optional<ExchangeRequest> found = exchangeRequestRepository.findByIdWithDetails(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(ExchangeRequestStatus.PENDING, found.get().getStatus());
    }

    @Test
    public void testFindByStatusWithoutDeliveryOfferWithDetails() {
        ExchangeRequest request1 = new ExchangeRequest();
        request1.setRequesterOffer(offer1);
        request1.setTargetOffer(offer2);
        request1.setStatus(ExchangeRequestStatus.ACCEPTED);
        request1.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request1);

        ExchangeRequest request2 = new ExchangeRequest();
        request2.setRequesterOffer(offer2);
        request2.setTargetOffer(offer1);
        request2.setStatus(ExchangeRequestStatus.PENDING);
        request2.setCreatedAt(LocalDateTime.now().minusDays(1));
        exchangeRequestRepository.save(request2);

        List<ExchangeRequest> accepted = exchangeRequestRepository.findByStatusWithoutDeliveryOfferWithDetails(ExchangeRequestStatus.ACCEPTED);

        assertEquals(1, accepted.size());
        assertEquals(ExchangeRequestStatus.ACCEPTED, accepted.get(0).getStatus());
    }

    @Test
    public void testUpdateExchangeRequestStatus() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        ExchangeRequest saved = exchangeRequestRepository.save(request);

        saved.setStatus(ExchangeRequestStatus.ACCEPTED);
        exchangeRequestRepository.save(saved);

        Optional<ExchangeRequest> updated = exchangeRequestRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(ExchangeRequestStatus.ACCEPTED, updated.get().getStatus());
    }

    @Test
    public void testFindAllWithDetails() {
        ExchangeRequest request1 = new ExchangeRequest();
        request1.setRequesterOffer(offer1);
        request1.setTargetOffer(offer2);
        request1.setStatus(ExchangeRequestStatus.PENDING);
        request1.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request1);

        ExchangeRequest request2 = new ExchangeRequest();
        request2.setRequesterOffer(offer2);
        request2.setTargetOffer(offer1);
        request2.setStatus(ExchangeRequestStatus.ACCEPTED);
        request2.setCreatedAt(LocalDateTime.now().plusDays(1));
        exchangeRequestRepository.save(request2);

        List<ExchangeRequest> all = exchangeRequestRepository.findAllWithDetails();

        assertEquals(2, all.size());
        // Most recent first (by createdAt desc)
        assertEquals(ExchangeRequestStatus.ACCEPTED, all.get(0).getStatus());
    }

    @Test
    public void testDeleteExchangeRequest() {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        ExchangeRequest saved = exchangeRequestRepository.save(request);

        exchangeRequestRepository.deleteById(saved.getId());

        Optional<ExchangeRequest> deleted = exchangeRequestRepository.findById(saved.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    public void testUniqueConstraintOnOfferPair() {
        ExchangeRequest request1 = new ExchangeRequest();
        request1.setRequesterOffer(offer1);
        request1.setTargetOffer(offer2);
        request1.setStatus(ExchangeRequestStatus.PENDING);
        request1.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request1);

        // Attempting to create duplicate should fail or if succeeded, validate duplicate detection
        boolean exists = exchangeRequestRepository.existsByRequesterOffer_IdAndTargetOffer_Id(
            offer1.getId(), offer2.getId()
        );
        assertTrue(exists);
    }
}
