package com.example.project.controller;

import com.example.project.entity.Book;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.Offer;
import com.example.project.entity.OfferStatus;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.notification.service.NotificationService;
import com.example.project.repository.BookRepository;
import com.example.project.repository.DeliveryOfferRepository;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.OfferRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ExchangeRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private DeliveryOfferRepository deliveryOfferRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NotificationService notificationService;

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

        deliveryOfferRepository.deleteAll();
        exchangeRequestRepository.deleteAll();
        offerRepository.deleteAll();
        userRepository.deleteAll();
        bookRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);

        // Create users
        user1 = new User();
        user1.setName("user1");
        user1.setEmail("user1@test.com");
        user1.setPassword(passwordEncoder.encode("password"));
        user1.setRoles(Set.of(userRole));
        userRepository.save(user1);

        user2 = new User();
        user2.setName("user2");
        user2.setEmail("user2@test.com");
        user2.setPassword(passwordEncoder.encode("password"));
        user2.setRoles(Set.of(userRole));
        userRepository.save(user2);

        // Create books
        book1 = new Book();
        book1.setBookId("BOOK-1");
        book1.setTitle("Book 1");
        book1.setAuthor("Author 1");
        book1.setPublisher("Publisher 1");
        book1.setPublicationYear(2020);
        bookRepository.save(book1);

        book2 = new Book();
        book2.setBookId("BOOK-2");
        book2.setTitle("Book 2");
        book2.setAuthor("Author 2");
        book2.setPublisher("Publisher 2");
        book2.setPublicationYear(2021);
        bookRepository.save(book2);

        // Create offers
        offer1 = new Offer();
        offer1.setUser(user1);
        offer1.setBook(book1);
        offer1.setCondition(conditionOne);
        offer1.setNote("Slightly used");
        offer1.setStatus(OfferStatus.ACTIVE);
        offer1.setCreatedAt(LocalDateTime.now());
        offerRepository.save(offer1);

        offer2 = new Offer();
        offer2.setUser(user2);
        offer2.setBook(book2);
        offer2.setCondition(conditionTwo);
        offer2.setNote("Like new");
        offer2.setStatus(OfferStatus.ACTIVE);
        offer2.setCreatedAt(LocalDateTime.now());
        offerRepository.save(offer2);
    }

    @Test
    public void testCreateExchangeRequest_Success() throws Exception {
        Map<String, Long> request = Map.of(
            "requesterOfferId", offer1.getId(),
            "targetOfferId", offer2.getId()
        );

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .with(user(user1.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    public void testCreateExchangeRequest_MissingFields() throws Exception {
        Map<String, Long> request = Map.of("requesterOfferId", offer1.getId());

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .with(user(user1.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("required")));
    }

    @Test
    public void testCreateExchangeRequest_OfferNotFound() throws Exception {
        Map<String, Long> request = Map.of(
            "requesterOfferId", 99999L,
            "targetOfferId", offer2.getId()
        );

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .with(user(user1.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateExchangeRequest_NotOwner() throws Exception {
        // user1 tries to use offer2 which belongs to user2
        Map<String, Long> request = Map.of(
            "requesterOfferId", offer2.getId(),
            "targetOfferId", offer1.getId()
        );

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .with(user(user1.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateExchangeRequest_OwnOffer() throws Exception {
        // user1 tries to request their own offer
        Offer offer1Copy = new Offer();
        offer1Copy.setUser(user1);
        offer1Copy.setBook(book2);
        offer1Copy.setCondition(resolveValidOfferCondition(0));
        offer1Copy.setNote("Another offer");
        offer1Copy.setStatus(OfferStatus.ACTIVE);
        offer1Copy.setCreatedAt(LocalDateTime.now());
        offerRepository.save(offer1Copy);

        Map<String, Long> request = Map.of(
            "requesterOfferId", offer1.getId(),
            "targetOfferId", offer1Copy.getId()
        );

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .with(user(user1.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("cannot request your own offer")));
    }

    @Test
    public void testAcceptExchangeRequest_Success() throws Exception {
        // Create a pending exchange request
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request);

        mockMvc.perform(put("/exchange-requests/{id}/accept", request.getId())
                .with(csrf())
                .with(user(user2.getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    public void testAcceptExchangeRequest_NotOwner() throws Exception {
        // Create a pending exchange request
        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequesterOffer(offer1);
        exchangeRequest.setTargetOffer(offer2);
        exchangeRequest.setStatus(ExchangeRequestStatus.PENDING);
        exchangeRequest.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(exchangeRequest);

        // user1 (not owner of target offer) tries to accept
        mockMvc.perform(put("/exchange-requests/{id}/accept", exchangeRequest.getId())
                .with(csrf())
                .with(user(user1.getEmail())))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testRejectExchangeRequest_Success() throws Exception {
        // Create a pending exchange request
        ExchangeRequest request = new ExchangeRequest();
        request.setRequesterOffer(offer1);
        request.setTargetOffer(offer2);
        request.setStatus(ExchangeRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(request);

        mockMvc.perform(put("/exchange-requests/{id}/reject", request.getId())
                .with(csrf())
                .with(user(user2.getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    public void testGetMyRequests_Success() throws Exception {
        // Create some exchange requests
        ExchangeRequest sent = new ExchangeRequest();
        sent.setRequesterOffer(offer1);
        sent.setTargetOffer(offer2);
        sent.setStatus(ExchangeRequestStatus.PENDING);
        sent.setCreatedAt(LocalDateTime.now());
        exchangeRequestRepository.save(sent);

        mockMvc.perform(get("/exchange-requests/my-requests")
                .with(csrf())
                .with(user(user1.getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sentRequests[0].status").value("PENDING"));
    }

    @Test
    public void testCreateExchangeRequest_Unauthorized() throws Exception {
        Map<String, Long> request = Map.of(
            "requesterOfferId", offer1.getId(),
            "targetOfferId", offer2.getId()
        );

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void testCreateExchangeRequest_InactiveOffer() throws Exception {
        // Set offer1 to RESERVED
        offer1.setStatus(OfferStatus.RESERVED);
        offerRepository.save(offer1);

        Map<String, Long> request = Map.of(
            "requesterOfferId", offer1.getId(),
            "targetOfferId", offer2.getId()
        );

        mockMvc.perform(post("/exchange-requests")
                .with(csrf())
                .with(user(user1.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("ACTIVE")));
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
