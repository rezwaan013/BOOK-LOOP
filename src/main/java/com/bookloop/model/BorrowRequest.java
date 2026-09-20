package com.bookloop.model;

import java.time.LocalDateTime;

/**
 * A request from one user to borrow another user's book for a chosen duration.
 */
public class BorrowRequest {

    private int id;
    private int bookId;
    private String bookTitle;       // transient — populated via SQL JOIN
    private int requesterId;
    private String requesterName;   // transient — populated via SQL JOIN
    private int ownerId;            // transient — populated via SQL JOIN
    private RequestStatus status;
    private int durationDays;       // 7, 14, or 21
    private LocalDateTime requestDate;
    private LocalDateTime dueDate;

    public BorrowRequest() { this.status = RequestStatus.PENDING; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public int getRequesterId() { return requesterId; }
    public void setRequesterId(int requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}
