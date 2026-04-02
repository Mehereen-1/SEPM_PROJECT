package com.example.project.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    return "redirect:/admin/dashboard";
                }
                if ("ROLE_DELIVERY_PARTNER".equals(role) || "ROLE_DELIVERY".equals(role)) {
                    return "redirect:/delivery/dashboard";
                }
            }
            return "redirect:/reader/dashboard";
        }

        return "index";
    }
    
    @GetMapping("/browse")
    public String browseBooksPage() {
        return "books-browse";
    }

    @GetMapping("/offers-browse")
    public String browseOffersPage() {
        return "offers-browse";
    }

    @GetMapping("/offers-create")
    public String createOfferPage() {
        return "offers-create";
    }

    @GetMapping("/my-offers")
    public String myOffersPage() {
        return "my-offers";
    }

    @GetMapping("/exchange-center")
    public String exchangeCenterPage() {
        return "exchange-center";
    }
}
