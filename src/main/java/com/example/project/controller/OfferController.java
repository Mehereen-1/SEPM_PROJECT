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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/offers")
public class OfferController {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OfferImageRepository offerImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

        @GetMapping
        public ResponseEntity<List<OfferBrowseResponse>> browseActiveOffers() {
        List<Offer> activeOffers = offerRepository.findByStatusWithDetails(OfferStatus.ACTIVE);
        if (activeOffers.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> offerIds = activeOffers.stream()
            .map(Offer::getId)
            .toList();

        List<OfferImage> allImages = offerImageRepository.findByOfferIdIn(offerIds);
        Map<Long, List<String>> imageUrlsByOfferId = new HashMap<>();

        for (OfferImage image : allImages) {
            Long imageOfferId = image.getOffer().getId();
            imageUrlsByOfferId.computeIfAbsent(imageOfferId, key -> new ArrayList<>())
                .add(image.getImageUrl());
        }

        List<OfferBrowseResponse> response = activeOffers.stream()
            .map(offer -> new OfferBrowseResponse(
                offer.getId(),
                offer.getBook().getTitle(),
                offer.getBook().getAuthor(),
                offer.getUser().getName(),
                offer.getCondition(),
                offer.getNote(),
                imageUrlsByOfferId.getOrDefault(offer.getId(), List.of())
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
        }

    @GetMapping("/my-active")
    public ResponseEntity<?> getMyActiveOffers() {
        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "User is not authenticated."));
        }

        Optional<User> userOptional = userRepository.findByEmail(currentUsername);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authenticated user not found."));
        }

        List<Offer> myActiveOffers = offerRepository.findByUserIdAndStatusWithDetails(userOptional.get().getId(), OfferStatus.ACTIVE);
        List<MyOfferResponse> response = myActiveOffers.stream()
            .map(offer -> new MyOfferResponse(
                offer.getId(),
                offer.getBook().getTitle(),
                offer.getCondition(),
                offer.getCreatedAt()
            ))
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyOffers() {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "User is not authenticated."));
        }

        List<Offer> myOffers = offerRepository.findByUserIdWithDetails(currentUser.get().getId());
        if (myOffers.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> offerIds = myOffers.stream().map(Offer::getId).toList();
        List<OfferImage> allImages = offerImageRepository.findByOfferIdIn(offerIds);
        Map<Long, List<String>> imageUrlsByOfferId = new HashMap<>();

        for (OfferImage image : allImages) {
            Long imageOfferId = image.getOffer().getId();
            imageUrlsByOfferId.computeIfAbsent(imageOfferId, key -> new ArrayList<>())
                .add(image.getImageUrl());
        }

        List<MyOfferDetailsResponse> response = myOffers.stream()
            .map(offer -> new MyOfferDetailsResponse(
                offer.getId(),
                offer.getBook().getBookId(),
                offer.getBook().getTitle(),
                offer.getBook().getAuthor(),
                offer.getCondition(),
                offer.getNote(),
                offer.getStatus().name(),
                offer.getCreatedAt(),
                imageUrlsByOfferId.getOrDefault(offer.getId(), List.of())
            ))
            .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{offerId}")
    public ResponseEntity<?> updateMyOffer(@PathVariable Long offerId, @RequestBody UpdateOfferRequest request) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "User is not authenticated."));
        }

        Optional<Offer> offerResult = offerRepository.findById(offerId);
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Offer not found."));
        }

        Offer offer = offerResult.get();
        if (!offer.getUser().getId().equals(currentUser.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You can only modify your own offers."));
        }

        if (request.condition() == null || request.condition().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "condition is required."));
        }

        offer.setCondition(request.condition().trim());
        offer.setNote(request.note() == null ? null : request.note().trim());
        Offer updated = offerRepository.save(offer);

        return ResponseEntity.ok(new OfferResponse(
            updated.getId(),
            updated.getBook().getBookId(),
            updated.getUser().getId(),
            updated.getCondition(),
            updated.getNote(),
            updated.getCreatedAt(),
            updated.getStatus().name()
        ));
    }

    @DeleteMapping("/{offerId}")
    public ResponseEntity<?> deleteMyOffer(@PathVariable Long offerId) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "User is not authenticated."));
        }

        Optional<Offer> offerResult = offerRepository.findById(offerId);
        if (offerResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Offer not found."));
        }

        Offer offer = offerResult.get();
        if (!offer.getUser().getId().equals(currentUser.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You can only delete your own offers."));
        }

        try {
            offerImageRepository.deleteByOffer_Id(offerId);
            offerRepository.delete(offer);
            return ResponseEntity.ok(Map.of("message", "Offer deleted successfully."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Offer cannot be deleted because it is referenced by active exchange data."));
        }
    }

    @PostMapping
    public ResponseEntity<?> createOffer(@RequestBody CreateOfferRequest request) {
        if (request.bookId() == null || request.bookId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "bookId is required."));
        }
        if (request.condition() == null || request.condition().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "condition is required."));
        }

        Optional<Book> bookOptional = bookRepository.findById(request.bookId());
        if (bookOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Book not found."));
        }

        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User is not authenticated."));
        }

        Optional<User> userOptional = userRepository.findByEmail(currentUsername);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authenticated user not found."));
        }

        Offer offer = new Offer();
        offer.setBook(bookOptional.get());
        offer.setUser(userOptional.get());
        offer.setCondition(request.condition());
        offer.setNote(request.note());
        offer.setCreatedAt(LocalDateTime.now());
        offer.setStatus(OfferStatus.ACTIVE);

        Offer savedOffer = offerRepository.save(offer);
        OfferResponse response = new OfferResponse(
            savedOffer.getId(),
            savedOffer.getBook().getBookId(),
            savedOffer.getUser().getId(),
            savedOffer.getCondition(),
            savedOffer.getNote(),
            savedOffer.getCreatedAt(),
            savedOffer.getStatus().name()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{offerId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadOfferImages(
            @PathVariable Long offerId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "images", required = false) MultipartFile[] images
    ) {
        Optional<Offer> offerOptional = offerRepository.findById(offerId);
        if (offerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Offer not found."));
        }

        MultipartFile[] uploadFiles = files != null ? files : images;
        if (uploadFiles == null || uploadFiles.length == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "At least one image is required."));
        }

        try {
            Path uploadDir = Paths.get("uploads", "offers", offerId.toString()).toAbsolutePath();
            Files.createDirectories(uploadDir);

            Offer offer = offerOptional.get();
            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : uploadFiles) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Only image files are allowed."));
                }

                String originalName = file.getOriginalFilename();
                String extension = extractExtension(originalName);
                String generatedName = UUID.randomUUID() + extension;

                Path targetPath = uploadDir.resolve(generatedName);
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                String imageUrl = "/uploads/offers/" + offerId + "/" + generatedName;

                OfferImage offerImage = new OfferImage();
                offerImage.setOffer(offer);
                offerImage.setImageUrl(imageUrl);
                offerImageRepository.save(offerImage);

                uploadedUrls.add(imageUrl);
            }

            if (uploadedUrls.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "No valid image files were uploaded."));
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("imageUrls", uploadedUrls));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload images."));
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot);
    }

    private Optional<User> getCurrentUser() {
        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            return Optional.empty();
        }
        return userRepository.findByEmail(currentUsername);
    }

    record CreateOfferRequest(
            String bookId,
            String condition,
            String note
    ) {
    }

        record UpdateOfferRequest(
            String condition,
            String note
        ) {
        }

        record OfferBrowseResponse(
            Long offerId,
            String bookTitle,
            String author,
            String ownerName,
            String condition,
            String note,
            List<String> imageUrls
        ) {
        }

        record OfferResponse(
            Long id,
            String bookId,
            Long userId,
            String condition,
            String note,
            LocalDateTime createdAt,
            String status
        ) {
        }

        record MyOfferResponse(
            Long offerId,
            String bookTitle,
            String condition,
            LocalDateTime createdAt
        ) {
        }

        record MyOfferDetailsResponse(
            Long offerId,
            String bookId,
            String bookTitle,
            String author,
            String condition,
            String note,
            String status,
            LocalDateTime createdAt,
            List<String> imageUrls
        ) {
        }
}