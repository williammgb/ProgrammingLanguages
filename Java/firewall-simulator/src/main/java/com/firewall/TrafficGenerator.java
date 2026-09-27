package com.firewall;

import java.time.Instant;
import java.util.List;
import java.util.Random;

// Makes fake packets so the firewall has something to filter.
public class TrafficGenerator {

    // Same seed gives the same packets every run, so a bug you hit once you can hit again.
    private final Random random;

    // Parsed once here rather than on every packet, because the text form is only
    // ever for us to read.
    private static final List<Integer> SOURCE_IPS = List.of(
            Ip.parse("10.0.0.5"),
            Ip.parse("10.0.0.11"),
            Ip.parse("192.168.1.20"),
            Ip.parse("203.0.113.7"));

    private static final int SERVER_IP = Ip.parse("93.184.216.34");

    // An entry listed twice is picked twice as often, so repeating a value is
    // how you weight it without writing weighting code.
    private static final List<Protocol> PROTOCOLS = List.of(
            Protocol.TCP, Protocol.TCP, Protocol.TCP, Protocol.UDP, Protocol.ICMP);

    private static final List<Integer> DEST_PORTS = List.of(
            443, 443, 443, 80, 80, 22, 53);

    public TrafficGenerator(long seed) {
        this.random = new Random(seed);
    }

    // One packet per call instead of a whole list, so the caller decides how many it wants.
    public Packet next() {
        Protocol protocol = pick(PROTOCOLS);

        // ICMP has no port field at all, so 0 stands in for "does not apply".
        int dstPort;
        if (protocol == Protocol.ICMP) {
            dstPort = 0;
        } else if (random.nextInt(5) == 0) {
            // One in five packets aims at a port nothing serves, so the default policy gets used.
            dstPort = 1024 + random.nextInt(64512);
        } else {
            dstPort = pick(DEST_PORTS);
        }

        return new Packet(
                pick(SOURCE_IPS),
                49152 + random.nextInt(16384),   // the ephemeral port range real clients pick from
                SERVER_IP,
                dstPort,
                protocol,
                Instant.now());
    }

    // nextInt(n) returns 0 to n-1, which is exactly the valid index range of a list of size n.
    private <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }
}
