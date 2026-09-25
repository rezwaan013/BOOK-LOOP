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
 * Fetches book metadata from the Open Library API.
 * Demonstrates <b>networking</b> (Java {@link HttpClient}) + <b>JSON parsing</b>
 * (Jackson {@link ObjectMapper} / {@link JsonNode}).
 * Two endpoints are used:
 * <ul>
 *   <li>Title search: {@code https://openlibrary.org/search.json?title=...} — no ISBN needed,
 *       used by the "Auto-fill" button on the Add Book form.</li>
 *   <li>ISBN lookup: {@code https://openlibrary.org/api/books?...} (legacy).</li>
 * </ul>
 * All network failures fall back gracefully to manually entered data.
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

    /**
     * Searches Open Library by title (and optionally author) without needing an ISBN.
     * HTTP GET {@code /search.json?title=...&author=...&limit=1}, then parses the
     * JSON {@code docs[0]} node into publisher / cover image / first-publish year.
     *
     * @return partially-filled Book, or empty on network failure / no match
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
            // --- JSON parsing with Jackson ---
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
                book.setDescription("First published " + year.asInt() + " (auto-filled from Open Library).");
            return Optional.of(book);
        } catch (Exception e) {
            LOGGER.warning("Open Library title search failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
