package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConnectionTrackerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    // The timestamp is a parameter here, so the tests can move time forward without waiting.
    private static Packet request(int srcPort, Instant at) {
        return new Packet(Ip.parse("10.0.0.5"), srcPort, Ip.parse("93.184.216.34"),
                443, Protocol.TCP, at);
    }

    @Test
    void aRememberedRequestMakesItsReplyEstablished() {
        ConnectionTracker tracker = new ConnectionTracker(Duration.ofSeconds(30), 100);
        Packet outbound = request(51344, START);

        assertFalse(tracker.isEstablished(outbound.reply(START)));
        tracker.remember(outbound);
        assertTrue(tracker.isEstablished(outbound.reply(START)));
    }

    @Test
    void anUnrelatedReplyIsNotEstablished() {
        ConnectionTracker tracker = new ConnectionTracker(Duration.ofSeconds(30), 100);
        tracker.remember(request(51344, START));

        // Same connection except for the port, which is enough to be a different connection.
        assertFalse(tracker.isEstablished(request(51345, START).reply(START)));
    }

    @Test
    void anEntryStopsCountingOnceTheTimeoutPasses() {
        ConnectionTracker tracker = new ConnectionTracker(Duration.ofSeconds(30), 100);
        Packet outbound = request(51344, START);
        tracker.remember(outbound);

        Packet lateReply = new Packet(outbound.dstIp(), outbound.dstPort(),
                outbound.srcIp(), outbound.srcPort(), Protocol.TCP, START.plusSeconds(31));

        assertFalse(tracker.isEstablished(lateReply));
    }

    @Test
    void theTableStopsGrowingAtItsCap() {
        ConnectionTracker tracker = new ConnectionTracker(Duration.ofSeconds(30), 2);

        for (int port = 50000; port < 50010; port++) {
            tracker.remember(request(port, START));
        }
        // Without this cap, anything that can send packets can exhaust our memory.
        assertEquals(2, tracker.size());
    }
}
