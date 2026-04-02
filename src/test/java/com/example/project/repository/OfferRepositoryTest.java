package com.example.project.repository;

import com.example.project.entity.Book;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OfferRepositoryTest {

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

    private User userOne;
    private User userTwo;

    private static final Pattern QUOTED_LITERAL_PATTERN = Pattern.compile("'([^']+)'");

    @BeforeEach
    void setUp() {
        Role memberRole = new Role();
        memberRole.setName("ROLE_USER");
        roleRepository.save(memberRole);

        userOne = new User();
        userOne.setName("User One");
        userOne.setEmail("offer-user1@example.com");
        userOne.setPassword("pw");
        userOne.setRoles(Set.of(memberRole));
        userRepository.save(userOne);

        userTwo = new User();
        userTwo.setName("User Two");
        userTwo.setEmail("offer-user2@example.com");
        userTwo.setPassword("pw");
        userTwo.setRoles(Set.of(memberRole));
        userRepository.save(userTwo);

        Book dune = new Book();
        dune.setBookId("offer-book-1");
        dune.setTitle("Dune");
        dune.setAuthor("Frank Herbert");
        bookRepository.save(dune);

        Book lotr = new Book();
        lotr.setBookId("offer-book-2");
        lotr.setTitle("LOTR");
        lotr.setAuthor("Tolkien");
        bookRepository.save(lotr);

        String validCondition = resolveValidOfferCondition(0);

        Offer olderActive = new Offer();
        olderActive.setUser(userOne);
        olderActive.setBook(dune);
        olderActive.setCondition(validCondition);
        olderActive.setNote("older");
        olderActive.setStatus(OfferStatus.ACTIVE);
        olderActive.setCreatedAt(LocalDateTime.now().minusDays(1));
        offerRepository.save(olderActive);

        Offer newerActive = new Offer();
        newerActive.setUser(userOne);
        newerActive.setBook(lotr);
        newerActive.setCondition(validCondition);
        newerActive.setNote("newer");
        newerActive.setStatus(OfferStatus.ACTIVE);
        newerActive.setCreatedAt(LocalDateTime.now());
        offerRepository.save(newerActive);

        Offer reserved = new Offer();
        reserved.setUser(userTwo);
        reserved.setBook(dune);
        reserved.setCondition(validCondition);
        reserved.setNote("reserved");
        reserved.setStatus(OfferStatus.RESERVED);
        reserved.setCreatedAt(LocalDateTime.now().minusHours(3));
        offerRepository.save(reserved);
    }

    @Test
    @DisplayName("findAllWithDetails returns offers sorted by createdAt desc")
    void findAllWithDetails_returnsOffersInDescendingCreateOrder() {
        List<Offer> offers = offerRepository.findAllWithDetails();

        assertEquals(3, offers.size());
        assertTrue(offers.get(0).getCreatedAt().isAfter(offers.get(1).getCreatedAt())
            || offers.get(0).getCreatedAt().isEqual(offers.get(1).getCreatedAt()));
        assertEquals("User One", offers.get(0).getUser().getName());
        assertFalse(offers.get(0).getBook().getTitle().isBlank());
    }

    @Test
    @DisplayName("findByStatusWithDetails returns only requested status")
    void findByStatusWithDetails_filtersByStatus() {
        List<Offer> activeOffers = offerRepository.findByStatusWithDetails(OfferStatus.ACTIVE);

        assertEquals(2, activeOffers.size());
        assertTrue(activeOffers.stream().allMatch(o -> o.getStatus() == OfferStatus.ACTIVE));
    }

    @Test
    @DisplayName("findByUserIdAndStatusWithDetails filters by owner and status")
    void findByUserIdAndStatusWithDetails_filtersByOwnerAndStatus() {
        List<Offer> result = offerRepository.findByUserIdAndStatusWithDetails(userOne.getId(), OfferStatus.ACTIVE);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getUser().getId().equals(userOne.getId())));
        assertTrue(result.stream().allMatch(o -> o.getStatus() == OfferStatus.ACTIVE));
    }

    @Test
    @DisplayName("findByUserIdWithDetails returns user offers in newest-first order")
    void findByUserIdWithDetails_returnsNewestFirst() {
        List<Offer> result = offerRepository.findByUserIdWithDetails(userOne.getId());

        assertEquals(2, result.size());
        assertTrue(result.get(0).getCreatedAt().isAfter(result.get(1).getCreatedAt())
            || result.get(0).getCreatedAt().isEqual(result.get(1).getCreatedAt()));
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
}
