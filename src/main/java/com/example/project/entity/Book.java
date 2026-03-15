package com.example.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    
    @Id
    @Column(length = 100)
    private String bookId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String series;
    
    @Column(nullable = false)
    private String author;
    
    @Column
    private Double rating;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column
    private String language;
    
    @Column(length = 50)
    private String isbn;
    
    @Column(columnDefinition = "TEXT")
    private String genres;
    
    @Column(columnDefinition = "TEXT")
    private String characters;
    
    @Column
    private Integer pages;
    
    @Column
    private String firstPublishDate;
    
    @Column(columnDefinition = "TEXT")
    private String awards;
    
    @Column(columnDefinition = "TEXT")
    private String coverImg;
}
