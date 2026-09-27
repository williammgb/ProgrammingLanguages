package com.firewall;

// How many packets per second one source may send through a rule. The same number is
// the burst size, so a quiet source can send a full second's worth at once.
public record RateLimit(int perSecond) {

    public RateLimit {
        if (perSecond <= 0) {
            throw new IllegalArgumentException("rate must be positive, got " + perSecond);
        }
    }

    public static RateLimit parse(String text) {
        if (!text.endsWith("/s")) {
            throw new IllegalArgumentException("rate must look like 50/s, got " + text);
        }
        return new RateLimit(Integer.parseInt(text.substring(0, text.length() - 2)));
    }

    @Override
    public String toString() {
        return perSecond + "/s";
    }
}
