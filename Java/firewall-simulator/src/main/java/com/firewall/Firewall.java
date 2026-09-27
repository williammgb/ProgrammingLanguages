package com.firewall;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

// Holds the rule list and decides what happens to each packet.
public class Firewall {

    // final: you can only assign a value to this variable once
    private final List<Rule> rules; // Rules cannot be accessed outside this class
    private final Action defaultPolicy;
    private final ConnectionTracker tracker;

    // One counter per rule, at the same index as the rule it belongs to.
    private final AtomicLongArray hits;
    private final AtomicLong defaultPolicyHits = new AtomicLong();
    private final AtomicLong rateLimited = new AtomicLong();

    // A record nested inside a class, used only as a map key. It gets equals and
    // hashCode for free, which is the whole reason it exists.
    private record BucketKey(int ruleIndex, int srcIp) {
    }

    private final Map<BucketKey, TokenBucket> buckets = new ConcurrentHashMap<>();

    public Firewall(List<Rule> rules, Action defaultPolicy) {
        this(rules, defaultPolicy, new ConnectionTracker(Duration.ofSeconds(30), 10_000));
    }

    // Takes the tracker so a test can hand in a short timeout or a tiny table.
    public Firewall(List<Rule> rules, Action defaultPolicy, ConnectionTracker tracker) {
        // Copy the list so the caller cannot add or remove rules after this point.
        this.rules = List.copyOf(rules); // Always returns immutable List
        this.defaultPolicy = defaultPolicy;
        this.tracker = tracker;
        this.hits = new AtomicLongArray(this.rules.size());
    }

    // Checks the rules top to bottom and returns the action of the first one that matches,
    // so a lower rule never runs on traffic an earlier rule already claimed.
    public Action evaluate(Packet packet) {
        // Asked once, before the loop, because the answer cannot change while we walk it.
        boolean established = tracker.isEstablished(packet);

        // Indexed rather than for-each, because the counter is found by position.
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            if (rule.matches(packet, established)) {
                hits.incrementAndGet(i);
                Action verdict = rule.action();
                // The rule matched, but a sender over its rate is denied anyway.
                if (rule.rateLimit() != null && !withinRate(i, rule.rateLimit(), packet)) {
                    rateLimited.incrementAndGet();
                    verdict = Action.DENY;
                }
                return record(packet, verdict);
            }
        }
        // No rule said anything about this packet, so fall back to the default.
        defaultPolicyHits.incrementAndGet();
        return record(packet, defaultPolicy);
    }

    public List<RuleHits> hitCounts() {
        List<RuleHits> counts = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            counts.add(new RuleHits(rules.get(i), hits.get(i)));
        }
        return counts;
    }

    public long defaultPolicyHits() {
        return defaultPolicyHits.get();
    }

    public long rateLimited() {
        return rateLimited.get();
    }

    // Each source gets its own bucket per rule, so one noisy sender cannot use up
    // everybody else's allowance.
    private boolean withinRate(int ruleIndex, RateLimit limit, Packet packet) {
        TokenBucket bucket = buckets.computeIfAbsent(
                new BucketKey(ruleIndex, packet.srcIp()),
                key -> new TokenBucket(limit.perSecond(), limit.perSecond(), packet.timestamp()));
        return bucket.tryTake(packet.timestamp());
    }

    public Action defaultPolicy() {
        return defaultPolicy;
    }

    // Allowing a packet changes what the firewall will allow next, so evaluate() is
    // no longer a function of its argument alone.
    private Action record(Packet packet, Action verdict) {
        if (verdict == Action.ALLOW) {
            tracker.remember(packet);
        }
        return verdict;
    }
}
