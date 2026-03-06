package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "books")
@Data
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String author;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String isbn;
    
    @Enumerated(EnumType.STRING)
    private BookCondition condition;
    
    @Column(nullable = false)
    private String owner;
    
    public enum BookCondition {
        NEW, GOOD, FAIR, POOR
    }
}
