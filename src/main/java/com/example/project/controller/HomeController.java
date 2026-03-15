package com.example.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
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
}
