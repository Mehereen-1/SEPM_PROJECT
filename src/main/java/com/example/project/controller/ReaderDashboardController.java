package com.example.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.ExchangeRequestStatus;
import com.example.project.entity.User;
import com.example.project.repository.ExchangeRequestRepository;
import com.example.project.repository.OfferRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import com.example.project.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Controller
public class ReaderDashboardController {

    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @GetMapping("/reader/dashboard")
    public String readerDashboard(Model model) {
        String currentEmail = securityUtil.getCurrentUsername();
        
        if (currentEmail != null) {
            User user = userRepository.findByEmail(currentEmail).orElse(null);
            if (user != null) {
                ProfileStats stats = calculateStats(user);
                model.addAttribute("user", user);
                model.addAttribute("userName", user.getName());
                model.addAttribute("userEmail", user.getEmail());

                model.addAttribute("booksShared", stats.booksShared());
                model.addAttribute("readersReached", stats.readersReached());
                model.addAttribute("activeExchanges", stats.activeExchanges());
            }
        }
        
        model.addAttribute("offers", java.util.Collections.emptyList());
        return "reader-dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        ProfileStats stats = calculateStats(user);
        model.addAttribute("user", user);
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());

        ProfileUpdateRequest profileForm = new ProfileUpdateRequest();
        profileForm.setFirstName(user.getFirstName() != null ? user.getFirstName() : splitName(user.getName())[0]);
        profileForm.setLastName(user.getLastName() != null ? user.getLastName() : splitName(user.getName())[1]);
        profileForm.setGender(user.getGender());
        profileForm.setDateOfBirth(user.getDateOfBirth());
        profileForm.setPhoneNumber1(user.getPhoneNumber1());
        profileForm.setPhoneNumber2(user.getPhoneNumber2());
        profileForm.setLatitude(user.getLatitude());
        profileForm.setLongitude(user.getLongitude());
        profileForm.setAddress(user.getAddress());
        model.addAttribute("profileForm", profileForm);

        model.addAttribute("booksShared", stats.booksShared());
        model.addAttribute("readersReached", stats.readersReached());
        model.addAttribute("activeExchanges", stats.activeExchanges());

        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateRequest profileForm, BindingResult validation, Model model, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            redirectAttributes.addFlashAttribute("profileError", "You must be logged in to update your profile.");
            return "redirect:/login";
        }

        if (validation.hasErrors()) {
            ProfileStats stats = calculateStats(user);
            model.addAttribute("user", user);
            model.addAttribute("userName", user.getName());
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("booksShared", stats.booksShared());
            model.addAttribute("readersReached", stats.readersReached());
            model.addAttribute("activeExchanges", stats.activeExchanges());
            model.addAttribute("profileForm", profileForm);
            model.addAttribute("profileError", "Please correct the highlighted errors.");
            return "profile";
        }

        userService.updateProfile(
            user,
            profileForm.getFirstName(),
            profileForm.getLastName(),
            profileForm.getGender(),
            profileForm.getDateOfBirth(),
            profileForm.getPhoneNumber1(),
            profileForm.getPhoneNumber2(),
            profileForm.getLatitude(),
            profileForm.getLongitude(),
            profileForm.getAddress()
        );
        redirectAttributes.addFlashAttribute("profileMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }

    private User getCurrentUser() {
        String currentEmail = securityUtil.getCurrentUsername();
        if (currentEmail == null || "anonymousUser".equals(currentEmail)) {
            return null;
        }
        return userRepository.findByEmail(currentEmail).orElse(null);
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] {"", ""};
        }

        String[] tokens = fullName.trim().split("\\s+", 2);
        if (tokens.length == 1) {
            return new String[] {tokens[0], ""};
        }
        return tokens;
    }

    private ProfileStats calculateStats(User user) {
        long booksShared = offerRepository.findByUserIdWithDetails(user.getId()).size();

        java.util.List<ExchangeRequest> sent = exchangeRequestRepository.findSentByUserIdWithDetails(user.getId());
        java.util.List<ExchangeRequest> received = exchangeRequestRepository.findReceivedByUserIdWithDetails(user.getId());

        long activeSent = sent.stream()
            .filter(er -> er.getStatus() == ExchangeRequestStatus.PENDING || er.getStatus() == ExchangeRequestStatus.ACCEPTED)
            .count();
        long activeReceived = received.stream()
            .filter(er -> er.getStatus() == ExchangeRequestStatus.PENDING || er.getStatus() == ExchangeRequestStatus.ACCEPTED)
            .count();

        java.util.Set<Long> interactedUsers = new java.util.HashSet<>();
        for (ExchangeRequest er : sent) {
            interactedUsers.add(er.getTargetOffer().getUser().getId());
        }
        for (ExchangeRequest er : received) {
            interactedUsers.add(er.getRequesterOffer().getUser().getId());
        }

        return new ProfileStats(booksShared, activeSent + activeReceived, interactedUsers.size());
    }

    private record ProfileStats(long booksShared, long activeExchanges, long readersReached) {}

    public static class ProfileUpdateRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        private String gender;

        private java.time.LocalDate dateOfBirth;

        @NotBlank(message = "Primary contact number is required")
        private String phoneNumber1;

        private String phoneNumber2;

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be between -90 and 90")
        private Double latitude;

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be between -180 and 180")
        private Double longitude;

        private String address;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public java.time.LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(java.time.LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getPhoneNumber1() {
            return phoneNumber1;
        }

        public void setPhoneNumber1(String phoneNumber1) {
            this.phoneNumber1 = phoneNumber1;
        }

        public String getPhoneNumber2() {
            return phoneNumber2;
        }

        public void setPhoneNumber2(String phoneNumber2) {
            this.phoneNumber2 = phoneNumber2;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
