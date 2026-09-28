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
 * Fetches book metadata from the Open Library API by title.
 * Demonstrates networking (Java HttpClient) + JSON parsing
 * (Jackson ObjectMapper / JsonNode).
 * Endpoint: https://openlibrary.org/search.json?title=...&limit=1
 * All network failures fall back gracefully to manually entered data.
 */
public class BookApiService {

    private static final Logger LOGGER = Logger.getLogger(BookApiService.class.getName());
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Searches Open Library by title (and optionally author).
     * HTTP GETs search.json, then parses docs[0] into publisher,
     * cover image URL and first-publish year.
     *
     * @return partially-filled Book, or empty on failure / no match
     */
    public Optional<Book> fetchByTitle(String title, String author) {
        try {
            String q = "title=" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
            if (author != null && !author.isBlank())
                q += "&author=" + java.net.URLEncoder.encode(author.trim(), java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://openlibrary.org/search.json?" + q + "&limit=1";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOGGER.warning("Open Library search returned HTTP " + resp.statusCode());
                return Optional.empty();
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode docs = root.get("docs");
            if (docs == null || !docs.isArray() || docs.isEmpty()) return Optional.empty();
            JsonNode first = docs.get(0);

            Book book = new Book();
            JsonNode pubs = first.get("publisher");
            if (pubs != null && pubs.isArray() && !pubs.isEmpty())
                book.setPublisher(pubs.get(0).asText());
            JsonNode coverId = first.get("cover_i");
            if (coverId != null && coverId.isNumber())
                book.setCoverUrl("https://covers.openlibrary.org/b/id/" + coverId.asLong() + "-M.jpg");
            JsonNode year = first.get("first_publish_year");
            if (year != null && year.isNumber())
                book.setDescription("First published " + year.asInt() + " (via Open Library).");
            return Optional.of(book);
        } catch (Exception e) {
            LOGGER.warning("Open Library title search failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
