package com.firewall;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Remembers the connections we allowed, so the answer to one can be let back in
// without a rule that opens the port to everybody.
public class ConnectionTracker {

    // Maps the reply we are waiting for to the moment we started waiting.
    // ConcurrentHashMap because multiple threads read and write this at once;
    // a plain HashMap in this case would overwrite rather than throwing.
    private final Map<ConnectionKey, Instant> expected = new ConcurrentHashMap<>();

    private final Duration timeout;
    private final int maxEntries;

    public ConnectionTracker(Duration timeout, int maxEntries) {
        this.timeout = timeout;
        this.maxEntries = maxEntries;
    }

    public void remember(Packet allowed) {
        Instant now = allowed.timestamp();
        dropExpired(now);

        // Without a ceiling, opening connections and never finishing them runs us out
        // of memory. Two threads can both pass this check, so the cap is approximate.
        if (expected.size() >= maxEntries) {
            return;
        }
        expected.put(ConnectionKey.of(allowed.reply(now)), now);
    }

    public boolean isEstablished(Packet packet) {
        // Check if we expect this packet as a reply
        Instant started = expected.get(ConnectionKey.of(packet));
        if (started == null) {
            return false;
        }
        // Checked again here because dropExpired only runs when something is remembered.
        return !isExpired(started, packet.timestamp());
    }

    public int size() {
        return expected.size();
    }

    private void dropExpired(Instant now) {
        // Removing through the iterator is the only safe way to delete while looping
        // over a map; expected.remove(key) inside the loop throws instead.
        Iterator<Map.Entry<ConnectionKey, Instant>> entries = expected.entrySet().iterator();
        while (entries.hasNext()) {
            if (isExpired(entries.next().getValue(), now)) {
                entries.remove();
            }
        }
    }

    private boolean isExpired(Instant started, Instant now) {
        return Duration.between(started, now).compareTo(timeout) > 0;
    }
}
