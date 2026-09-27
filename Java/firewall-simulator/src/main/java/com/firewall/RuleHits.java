package com.firewall;

// One rule and how many packets it decided. A count of zero is the interesting case.
public record RuleHits(Rule rule, long hits) {
}
