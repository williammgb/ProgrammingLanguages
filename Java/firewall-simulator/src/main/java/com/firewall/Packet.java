package com.firewall;

import java.time.Instant;

public record Packet(
        int srcIp,
        int srcPort,
        int dstIp,
        int dstPort,
        Protocol protocol,
        Instant timestamp) {

    // The answer to this packet: same connection, sender and receiver swapped.
    // The time is a parameter rather than Instant.now(), so nothing here depends on
    // a clock a test cannot control.
    public Packet reply(Instant at) {
        return new Packet(dstIp, dstPort, srcIp, srcPort, protocol, at);
    }
}
