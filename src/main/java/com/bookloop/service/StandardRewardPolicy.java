package com.bookloop.service;

/**
 * Default reward economy: +50 on signup, +10 per book added,
 * 1 point per borrowed day (7 days = 7 pts, 14 = 14, 21 = 21).
 * Demonstrates <b>polymorphism</b> — used everywhere through the
 * {@link RewardPolicy} interface type, not this concrete class.
 */
public class StandardRewardPolicy implements RewardPolicy {

    @Override
    public int signupBonus() { return 50; }

    @Override
    public int pointsForAddingBook() { return 10; }

    @Override
    public int costToBorrow(int durationDays) { return durationDays; }
}
