package com.example.project.admin.repository;

import com.example.project.entity.Book;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.BookRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.OfferRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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
@DisplayName("Admin Repository Layer Tests")
class AdminRepositoryLayerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User adminUser;
    private User regularUser;
    private User secondUser;
    private Offer olderOffer;
    private Offer newerOffer;

    private static final Pattern QUOTED_LITERAL_PATTERN = Pattern.compile("'([^']+)'");

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin.repo@example.com");
        adminUser.setPassword("pw");
        adminUser.setActive(true);
        adminUser.setRoles(Set.of(adminRole, userRole));
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setName("Regular One");
        regularUser.setEmail("regular1.repo@example.com");
        regularUser.setPassword("pw");
        regularUser.setActive(true);
        regularUser.setRoles(Set.of(userRole));
        userRepository.save(regularUser);

        secondUser = new User();
        secondUser.setName("Regular Two");
        secondUser.setEmail("regular2.repo@example.com");
        secondUser.setPassword("pw");
        secondUser.setActive(true);
        secondUser.setRoles(Set.of(userRole));
        userRepository.save(secondUser);

        Book bookOne = new Book();
        bookOne.setBookId("admin-repo-book-1");
        bookOne.setTitle("Admin Book One");
        bookOne.setAuthor("Author One");
        bookRepository.save(bookOne);

        Book bookTwo = new Book();
        bookTwo.setBookId("admin-repo-book-2");
        bookTwo.setTitle("Admin Book Two");
        bookTwo.setAuthor("Author Two");
        bookRepository.save(bookTwo);

        String validCondition = resolveValidOfferCondition(0);

        olderOffer = new Offer();
        olderOffer.setUser(regularUser);
        olderOffer.setBook(bookOne);
        olderOffer.setCondition(validCondition);
        olderOffer.setNote("older-offer");
        olderOffer.setStatus(OfferStatus.ACTIVE);
        olderOffer.setCreatedAt(LocalDateTime.now().minusDays(2));
        offerRepository.save(olderOffer);

        newerOffer = new Offer();
        newerOffer.setUser(secondUser);
        newerOffer.setBook(bookTwo);
        newerOffer.setCondition(validCondition);
        newerOffer.setNote("newer-offer");
        newerOffer.setStatus(OfferStatus.ACTIVE);
        newerOffer.setCreatedAt(LocalDateTime.now().minusDays(1));
        offerRepository.save(newerOffer);
    }

    @Test
    @DisplayName("findByAnyRoleNames returns users matching admin role")
    void findByAnyRoleNames_returnsUsersMatchingRoleNames() {
        List<User> admins = userRepository.findByAnyRoleNames(Set.of("ADMIN"));

        assertEquals(1, admins.size());
        assertEquals("admin.repo@example.com", admins.get(0).getEmail());
    }

    @Test
    @DisplayName("findAllWithDetails on offers returns newest first with user and book populated")
    void offerFindAllWithDetails_returnsNewestFirstWithDetails() {
        List<Offer> offers = offerRepository.findAllWithDetails();

        assertEquals(2, offers.size());
        assertTrue(offers.get(0).getCreatedAt().isAfter(offers.get(1).getCreatedAt())
            || offers.get(0).getCreatedAt().isEqual(offers.get(1).getCreatedAt()));
        assertFalse(offers.get(0).getUser().getName().isBlank());
        assertFalse(offers.get(0).getBook().getTitle().isBlank());
    }

    @Test
    @DisplayName("findAllWithDetails on exchange requests returns newest first with related details")
    void exchangeFindAllWithDetails_returnsNewestFirstWithDetails() {
        ExchangeRequest older = new ExchangeRequest();
        older.setRequesterOffer(olderOffer);
        older.setTargetOffer(newerOffer);
        older.setStatus(ExchangeRequestStatus.PENDING);
        older.setCreatedAt(LocalDateTime.now().minusHours(2));
        exchangeRequestRepository.save(older);

        ExchangeRequest newer = new ExchangeRequest();
        newer.setRequesterOffer(newerOffer);
        newer.setTargetOffer(olderOffer);
        newer.setStatus(ExchangeRequestStatus.ACCEPTED);
        newer.setCreatedAt(LocalDateTime.now().minusHours(1));
        exchangeRequestRepository.save(newer);

        List<ExchangeRequest> requests = exchangeRequestRepository.findAllWithDetails();

        assertEquals(2, requests.size());
        assertTrue(requests.get(0).getCreatedAt().isAfter(requests.get(1).getCreatedAt())
            || requests.get(0).getCreatedAt().isEqual(requests.get(1).getCreatedAt()));
        assertFalse(requests.get(0).getRequesterOffer().getUser().getName().isBlank());
        assertFalse(requests.get(0).getTargetOffer().getBook().getTitle().isBlank());
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
