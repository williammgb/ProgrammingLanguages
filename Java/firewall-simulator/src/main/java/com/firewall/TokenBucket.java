package com.firewall;

import java.time.Duration;
import java.time.Instant;

// A bucket that holds up to `capacity` tokens and refills at a steady rate. Each packet
// takes one token; an empty bucket means the sender is over its limit.
public class TokenBucket {

    private final double capacity;
    private final double refillPerSecond;
    private double tokens;
    private Instant lastRefill;

    public TokenBucket(double capacity, double refillPerSecond, Instant now) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity;
        this.lastRefill = now;
    }

    // synchronized is Java's lock(this): one thread at a time, because refilling and
    // taking are several steps on shared fields.
    public synchronized boolean tryTake(Instant now) {
        double elapsedSeconds = Duration.between(lastRefill, now).toNanos() / 1_000_000_000.0;

        // Workers finish out of order, so a packet can carry an older time than the last
        // one seen; refilling on a negative gap would take tokens away.
        if (elapsedSeconds > 0) {
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefill = now;
        }

        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }
}
