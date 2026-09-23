package com.bookloop.service;

import com.bookloop.dao.BookDAO;
import com.bookloop.dao.BorrowRequestDAO;
import com.bookloop.model.Book;
import com.bookloop.model.BorrowRequest;
import com.bookloop.model.RequestStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for the borrow request lifecycle:
 * request -> accept/decline -> return.
 */
public class BorrowService {

    private final BorrowRequestDAO requestDAO       = new BorrowRequestDAO();
    private final BookDAO          bookDAO          = new BookDAO();
    private final NotificationService notifService  = new NotificationService();

    /**
     * Creates a PENDING borrow request and notifies the book owner.
     *
     * @param bookId        the book to borrow
     * @param requesterId   id of the requesting user
     * @param durationDays  7, 14, or 21 days
     * @param requesterName display name of the requester (for notification text)
     */
    public BorrowRequest requestBorrow(int bookId, int requesterId,
                                       int durationDays, String requesterName)
            throws SQLException {
        Optional<Book> bookOpt = bookDAO.findById(bookId);
        if (bookOpt.isEmpty())          throw new IllegalArgumentException("Book not found.");
        Book book = bookOpt.get();
        if (!book.isAvailable())        throw new IllegalStateException("This book is not currently available.");
        if (book.getOwnerId() == requesterId) throw new IllegalArgumentException("You cannot borrow your own book.");

        // Reward cost: 1 point per day (7 days = 7 pts, 14 = 14, 21 = 21)
        int cost = durationDays;
        com.bookloop.dao.UserDAO userDAO = new com.bookloop.dao.UserDAO();
        int balance = userDAO.findById(requesterId).map(com.bookloop.model.User::getRewardPoints).orElse(0);
        if (balance < cost)
            throw new IllegalStateException("Not enough reward points. Need " + cost + " pts, you have " + balance + " pts.");
        userDAO.addPoints(requesterId, -cost);
        com.bookloop.model.User current = com.bookloop.util.SessionManager.getCurrentUser();
        if (current != null && current.getId() == requesterId) current.setRewardPoints(balance - cost);

        BorrowRequest req = new BorrowRequest();
        req.setBookId(bookId);
        req.setRequesterId(requesterId);
        req.setDurationDays(durationDays);
        req.setStatus(RequestStatus.PENDING);
        req.setRequestDate(LocalDateTime.now());
        req.setDueDate(LocalDateTime.now().plusDays(durationDays));
        requestDAO.save(req);

        notifService.createNotification(
                book.getOwnerId(),
                requesterName + " wants to borrow your book: \"" + book.getTitle() + "\"");
        return req;
    }

    /**
     * Owner accepts a request: status -> ACCEPTED, book -> unavailable,
     * requester notified.
     */
    public void acceptRequest(int requestId, String ownerName) throws SQLException {
        BorrowRequest req = getOrThrow(requestId);
        requestDAO.updateStatus(requestId, RequestStatus.ACCEPTED);
        bookDAO.updateAvailability(req.getBookId(), false);
        String due = req.getDueDate() != null ? req.getDueDate().toLocalDate().toString() : "TBD";
        notifService.createNotification(
                req.getRequesterId(),
                ownerName + " accepted your request for \"" + req.getBookTitle()
                        + "\". Due back by " + due + ".");
    }

    /**
     * Owner declines a request: status -> DECLINED, requester notified.
     */
    public void declineRequest(int requestId, String ownerName) throws SQLException {
        BorrowRequest req = getOrThrow(requestId);
        requestDAO.updateStatus(requestId, RequestStatus.DECLINED);
        notifService.createNotification(
                req.getRequesterId(),
                ownerName + " declined your request for \"" + req.getBookTitle() + "\".");
    }

    /**
     * Marks the request as RETURNED and makes the book available again.
     */
    public void returnBook(int requestId) throws SQLException {
        BorrowRequest req = getOrThrow(requestId);
        requestDAO.updateStatus(requestId, RequestStatus.RETURNED);
        bookDAO.updateAvailability(req.getBookId(), true);
    }

    /** Returns all incoming requests for books owned by {@code ownerId}. */
    public List<BorrowRequest> getIncomingRequests(int ownerId) throws SQLException {
        return requestDAO.findIncomingForOwner(ownerId);
    }

    /** Returns all requests sent by {@code requesterId}. */
    public List<BorrowRequest> getOutgoingRequests(int requesterId) throws SQLException {
        return requestDAO.findByRequester(requesterId);
    }

    private BorrowRequest getOrThrow(int requestId) throws SQLException {
        return requestDAO.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request #" + requestId + " not found."));
    }
}
