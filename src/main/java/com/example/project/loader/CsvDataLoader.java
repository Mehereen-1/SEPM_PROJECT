package com.example.project.loader;

import com.example.project.entity.Book;
import com.example.project.repository.BookRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class CsvDataLoader implements CommandLineRunner {

    private static final String[] CSV_HEADERS = {
            "bookId", "title", "series", "author", "rating", "description", "language", "isbn",
            "genres", "characters", "unused_10", "unused_11", "pages", "unused_13", "unused_14",
            "firstPublishDate", "awards", "unused_17", "unused_18", "unused_19", "unused_20", "coverImg"
    };
    
    @Autowired
    private BookRepository bookRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if books already exist in database
        if (bookRepository.count() > 0) {
            System.out.println("Books already loaded in database. Skipping CSV load.");
            return;
        }
        
        System.out.println("Loading books from CSV file...");
        
        ClassPathResource resource = new ClassPathResource("books.csv");
        
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader(CSV_HEADERS)
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            int loadedCount = 0;

            for (CSVRecord record : parser) {
                try {
                    Book book = parseBook(record);
                    if (book != null && !bookRepository.existsById(book.getBookId())) {
                        bookRepository.save(book);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + e.getMessage());
                }
            }
            
            System.out.println("Successfully loaded " + loadedCount + " books from CSV.");
            
        } catch (Exception e) {
            System.err.println("Error loading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Book parseBook(CSVRecord record) {
        try {
            String bookId = sanitize(getField(record, "bookId"));
            String title = sanitize(getField(record, "title"));

            if (bookId == null || title == null) {
                return null;
            }
            
            Book book = new Book();
            
            book.setBookId(bookId);
            book.setTitle(title);
            book.setSeries(sanitize(getField(record, "series")));
            book.setAuthor(sanitize(getField(record, "author")));
            
            try {
                String rating = sanitize(getField(record, "rating"));
                book.setRating(rating != null ? Double.parseDouble(rating) : null);
            } catch (NumberFormatException e) {
                book.setRating(null);
            }
            
            book.setDescription(sanitize(getField(record, "description")));
            book.setLanguage(sanitize(getField(record, "language")));
            book.setIsbn(sanitize(getField(record, "isbn")));
            book.setGenres(sanitize(getField(record, "genres")));
            book.setCharacters(sanitize(getField(record, "characters")));
            
            try {
                String pages = sanitize(getField(record, "pages"));
                book.setPages(pages != null ? Integer.parseInt(pages) : null);
            } catch (NumberFormatException e) {
                book.setPages(null);
            }
            
            book.setFirstPublishDate(sanitize(getField(record, "firstPublishDate")));
            book.setAwards(sanitize(getField(record, "awards")));
            book.setCoverImg(sanitize(getField(record, "coverImg")));
            
            return book;
            
        } catch (Exception e) {
            System.err.println("Error in parseBook: " + e.getMessage());
            return null;
        }
    }

    private String getField(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header) : null;
    }
    
    private String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().replaceAll("^\"|\"$", "");
    }
}
