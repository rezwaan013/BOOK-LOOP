package com.bookloop.service;

/**
 * Strategy <b>interface</b> for the reward-points economy.
 * Swapping in a different implementation changes the whole points
 * economy without touching {@link BookService} or {@link BorrowService}.
 */
public interface RewardPolicy {

    /** Points granted once at registration. */
    int signupBonus();

    /** Points granted when a user contributes a book. */
    int pointsForAddingBook();

    /**
     * Points charged to the borrower for a loan.
     * @param durationDays loan length (7, 14 or 21)
     */
    int costToBorrow(int durationDays);
}
