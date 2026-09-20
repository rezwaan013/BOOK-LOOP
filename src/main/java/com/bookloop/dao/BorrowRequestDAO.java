package com.bookloop.dao;

import com.bookloop.model.BorrowRequest;
import com.bookloop.model.RequestStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** CRUD and query operations for the borrow_requests table. */
public class BorrowRequestDAO {

    private final Connection con = DatabaseManager.getInstance().getConnection();

    /**
     * Inserts a new borrow request.
     * @return generated id
     */
    public int save(BorrowRequest req) throws SQLException {
        String sql = """
            INSERT INTO borrow_requests(book_id,requester_id,status,duration_days,due_date)
            VALUES(?,?,?,?,?)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getBookId());
            ps.setInt(2, req.getRequesterId());
            ps.setString(3, req.getStatus().name());
            ps.setInt(4, req.getDurationDays());
            ps.setString(5, req.getDueDate() != null ? req.getDueDate().toString() : null);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                int id = k.getInt(1);
                req.setId(id);
                return id;
            }
        }
    }

    /** Returns all requests made by a given requester (outgoing / My Requests). */
    public List<BorrowRequest> findByRequester(int requesterId) throws SQLException {
        String sql = joinedSelect() + " WHERE br.requester_id=? ORDER BY br.request_date DESC";
        return query(sql, ps -> ps.setInt(1, requesterId));
    }

    /** Returns all requests for books owned by the given user (incoming). */
    public List<BorrowRequest> findIncomingForOwner(int ownerId) throws SQLException {
        String sql = joinedSelect() + " WHERE b.owner_id=? ORDER BY br.request_date DESC";
        return query(sql, ps -> ps.setInt(1, ownerId));
    }

    /** Looks up a single request by id. */
    public Optional<BorrowRequest> findById(int id) throws SQLException {
        String sql = joinedSelect() + " WHERE br.id=?";
        List<BorrowRequest> r = query(sql, ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /** Updates the status of an existing request. */
    public void updateStatus(int requestId, RequestStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE borrow_requests SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    private String joinedSelect() {
        return """
            SELECT br.*, b.title AS book_title, b.owner_id,
                   u.full_name AS requester_name
            FROM borrow_requests br
            JOIN books b ON br.book_id = b.id
            JOIN users u ON br.requester_id = u.id
            """;
    }

    @FunctionalInterface
    private interface ParamSetter { void set(PreparedStatement ps) throws SQLException; }

    private List<BorrowRequest> query(String sql, ParamSetter setter) throws SQLException {
        List<BorrowRequest> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private BorrowRequest map(ResultSet rs) throws SQLException {
        BorrowRequest r = new BorrowRequest();
        r.setId(rs.getInt("id"));
        r.setBookId(rs.getInt("book_id"));
        r.setBookTitle(rs.getString("book_title"));
        r.setOwnerId(rs.getInt("owner_id"));
        r.setRequesterId(rs.getInt("requester_id"));
        r.setRequesterName(rs.getString("requester_name"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        r.setDurationDays(rs.getInt("duration_days"));
        String rd = rs.getString("request_date");
        if (rd != null) { try { r.setRequestDate(LocalDateTime.parse(rd.replace(" ","T"))); } catch (Exception ignored) {} }
        String dd = rs.getString("due_date");
        if (dd != null) { try { r.setDueDate(LocalDateTime.parse(dd)); } catch (Exception ignored) {} }
        return r;
    }
}
