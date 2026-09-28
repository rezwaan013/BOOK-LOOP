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
 * Fetches book metadata from the Open Library API by title, with a
 * bundled local catalog (/books.json) as fallback.
 * Demonstrates networking (Java HttpClient) + JSON parsing
 * (Jackson ObjectMapper / JsonNode) on both paths:
 * online JSON from https://openlibrary.org/search.json and
 * local JSON from the classpath.
 * Resolution order: exact local-catalog match -> live API ->
 * partial local-catalog match -> empty (fill manually).
 */
public class BookApiService {

    private static final Logger LOGGER = Logger.getLogger(BookApiService.class.getName());
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Resolves book metadata for a title.
     * Local catalog is checked first for exact title matches (fast,
     * offline-safe); otherwise the live Open Library API is queried;
     * otherwise a partial local match is tried.
     *
     * @return partially-filled Book, or empty on failure / no match
     */
    public Optional<Book> fetchByTitle(String title, String author) {
        Optional<Book> local = findInCatalog(title, true);
        if (local.isPresent()) return local;
        Optional<Book> online = fetchFromApi(title, author);
        if (online.isPresent()) return online;
        return findInCatalog(title, false);
    }

    /** Loads the bundled /books.json catalog (Jackson parses local JSON). */
    private java.util.List<JsonNode> loadCatalog() {
        try (java.io.InputStream in = BookApiService.class.getResourceAsStream("/books.json")) {
            if (in == null) return java.util.List.of();
            JsonNode root = mapper.readTree(in);
            if (root == null || !root.isArray()) return java.util.List.of();
            java.util.List<JsonNode> list = new java.util.ArrayList<>();
            root.forEach(list::add);
            return list;
        } catch (Exception e) {
            LOGGER.warning("Failed to read bundled books.json: " + e.getMessage());
            return java.util.List.of();
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Searches the local catalog by title.
     * @param exact true: normalized titles must equal; false: either contains the other
     */
    private Optional<Book> findInCatalog(String title, boolean exact) {
        String q = norm(title);
        if (q.isBlank()) return Optional.empty();
        for (JsonNode e : loadCatalog()) {
            String t = norm(e.path("title").asText(""));
            boolean hit = exact ? t.equals(q) : (!t.isBlank() && (t.contains(q) || q.contains(t)));
            if (!hit) continue;
            Book book = new Book();
            book.setPublisher(e.path("publisher").asText(""));
            book.setDescription(e.path("description").asText(""));
            String a = e.path("author").asText("");
            if (!a.isBlank() && !a.equalsIgnoreCase("Unknown")) book.setAuthor(a);
            return Optional.of(book);
        }
        return Optional.empty();
    }

    /**
     * Searches Open Library by title (and optionally author).
     * HTTP GETs search.json, then parses the top docs into publisher,
     * cover image URL and first-publish year.
     */
    private Optional<Book> fetchFromApi(String title, String author) {
        try {
            String q = "title=" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
            if (author != null && !author.isBlank())
                q += "&author=" + java.net.URLEncoder.encode(author.trim(), java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://openlibrary.org/search.json?" + q + "&limit=5";
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

            // Scan the top hits and merge: first non-empty publisher, cover and year win.
            // (docs[0] alone often lacks a publisher, e.g. for "1984".)
            Book book = new Book();
            for (JsonNode doc : docs) {
                if ((book.getPublisher() == null || book.getPublisher().isBlank())) {
                    JsonNode pubs = doc.get("publisher");
                    if (pubs != null && pubs.isArray() && !pubs.isEmpty()
                            && !pubs.get(0).asText().isBlank())
                        book.setPublisher(pubs.get(0).asText());
                }
                if (book.getCoverUrl() == null || book.getCoverUrl().isBlank()) {
                    JsonNode coverId = doc.get("cover_i");
                    if (coverId != null && coverId.isNumber())
                        book.setCoverUrl("https://covers.openlibrary.org/b/id/" + coverId.asLong() + "-M.jpg");
                }
                if (book.getDescription() == null || book.getDescription().isBlank()) {
                    JsonNode year = doc.get("first_publish_year");
                    if (year != null && year.isNumber())
                        book.setDescription("First published " + year.asInt() + " (via Open Library).");
                }
                if (book.getPublisher() != null && !book.getPublisher().isBlank()
                        && book.getCoverUrl() != null && !book.getCoverUrl().isBlank()) break;
            }
            return Optional.of(book);
        } catch (Exception e) {
            LOGGER.warning("Open Library title search failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
