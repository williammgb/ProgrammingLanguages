package com.firewall;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void aFullBucketAllowsExactlyItsCapacityThenRefuses() {
        TokenBucket bucket = new TokenBucket(2, 2, START);

        assertTrue(bucket.tryTake(START));
        assertTrue(bucket.tryTake(START));
        assertFalse(bucket.tryTake(START));
    }

    @Test
    void tokensComeBackAtTheRefillRate() {
        TokenBucket bucket = new TokenBucket(1, 2, START);
        assertTrue(bucket.tryTake(START));

        // 2 per second means one token every half second.
        assertFalse(bucket.tryTake(START.plusMillis(400)));
        assertTrue(bucket.tryTake(START.plusMillis(600)));
    }

    @Test
    void aLongPauseRefillsOnlyUpToCapacity() {
        TokenBucket bucket = new TokenBucket(2, 10, START);
        bucket.tryTake(START);
        bucket.tryTake(START);

        // An hour of silence is worth 36,000 tokens, but the bucket only holds 2.
        Instant later = START.plusSeconds(3600);
        assertTrue(bucket.tryTake(later));
        assertTrue(bucket.tryTake(later));
        assertFalse(bucket.tryTake(later));
    }

    @Test
    void aPacketFromThePastDoesNotDrainTheBucket() {
        TokenBucket bucket = new TokenBucket(1, 1, START.plusSeconds(10));

        assertTrue(bucket.tryTake(START));
        assertTrue(bucket.tryTake(START.plusSeconds(11)));
    }
}
