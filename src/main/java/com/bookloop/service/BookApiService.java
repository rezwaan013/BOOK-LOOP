package com.bookloop.service;

import com.bookloop.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Fetches book metadata from the Open Library API using an ISBN.
 * Endpoint: https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data
 * Falls back gracefully to manually entered data if the network call fails.
 */
public class BookApiService {

    private static final Logger LOGGER = Logger.getLogger(BookApiService.class.getName());
    private static final String BASE_URL = "https://openlibrary.org/api/books";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Attempts to retrieve cover URL, description, and publisher for the given ISBN.
     * @param isbn ISBN-10 or ISBN-13 (hyphens/spaces stripped automatically)
     * @return a partially-filled Book, or empty if the API call fails or returns no data
     */
    public Optional<Book> fetchBookDetails(String isbn) {
        String clean = isbn.replaceAll("[^0-9X]", "");
        String url   = BASE_URL + "?bibkeys=ISBN:" + clean + "&format=json&jscmd=data";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOGGER.warning("Open Library returned HTTP " + resp.statusCode());
                return Optional.empty();
            }
            return parse(resp.body(), clean);
        } catch (Exception e) {
            LOGGER.warning("Open Library fetch failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Book> parse(String json, String isbn) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.get("ISBN:" + isbn);
            if (data == null || data.isNull()) return Optional.empty();

            Book book = new Book();

            // Cover image
            JsonNode cover = data.get("cover");
            if (cover != null) {
                JsonNode large = cover.get("large");
                JsonNode medium = cover.get("medium");
                if (large  != null) book.setCoverUrl(large.asText());
                else if (medium != null) book.setCoverUrl(medium.asText());
            }

            // Publisher
            JsonNode pubs = data.get("publishers");
            if (pubs != null && pubs.isArray() && !pubs.isEmpty()) {
                JsonNode nameNode = pubs.get(0).get("name");
                if (nameNode != null) book.setPublisher(nameNode.asText());
            }

            // Description (Open Library stores it under 'notes' or as an object)
            JsonNode notes = data.get("notes");
            if (notes != null) {
                if (notes.isTextual()) {
                    book.setDescription(notes.asText());
                } else {
                    JsonNode val = notes.get("value");
                    if (val != null) book.setDescription(val.asText());
                }
            }

            return Optional.of(book);
        } catch (Exception e) {
            LOGGER.warning("Failed to parse Open Library JSON: " + e.getMessage());
            return Optional.empty();
        }
    }
}
