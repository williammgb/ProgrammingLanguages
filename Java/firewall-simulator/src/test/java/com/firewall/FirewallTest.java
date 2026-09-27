package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FirewallTest {

    private static final Cidr ANYWHERE = Cidr.parse("0.0.0.0/0");

    private static Packet tcpTo(String srcIp, int dstPort) {
        return new Packet(Ip.parse(srcIp), 51344, Ip.parse("93.184.216.34"),
                dstPort, Protocol.TCP, Instant.now());
    }

    private static Rule allow(String source, String ports) {
        return new Rule(Action.ALLOW, Protocol.TCP, Cidr.parse(source), PortRange.parse(ports), false, null);
    }

    private static Rule deny(String source, String ports) {
        return new Rule(Action.DENY, Protocol.TCP, Cidr.parse(source), PortRange.parse(ports), false, null);
    }

    @Test
    void firstMatchingRuleWins() {
        Firewall firewall = new Firewall(
                List.of(allow("10.0.0.0/8", "22"), deny("0.0.0.0/0", "22")),
                Action.DENY);

        assertEquals(Action.ALLOW, firewall.evaluate(tcpTo("10.0.0.5", 22)));
        assertEquals(Action.DENY, firewall.evaluate(tcpTo("203.0.113.7", 22)));
    }

    @Test
    void aBroadRuleAboveANarrowOneMakesTheNarrowOneDead() {
        Firewall firewall = new Firewall(
                List.of(new Rule(Action.ALLOW, Protocol.TCP, ANYWHERE, PortRange.ANY, false, null),
                        deny("0.0.0.0/0", "22")),
                Action.DENY);

        // The deny is never reached, which is the failure this test exists to make visible.
        assertEquals(Action.ALLOW, firewall.evaluate(tcpTo("203.0.113.7", 22)));
    }

    @Test
    void unmatchedTrafficFallsBackToTheDefaultPolicy() {
        Firewall denyByDefault = new Firewall(List.of(), Action.DENY);
        Firewall allowByDefault = new Firewall(List.of(), Action.ALLOW);

        assertEquals(Action.DENY, denyByDefault.evaluate(tcpTo("10.0.0.5", 8080)));
        assertEquals(Action.ALLOW, allowByDefault.evaluate(tcpTo("10.0.0.5", 8080)));
    }

    @Test
    void aShadowedRuleReportsZeroHits() {
        Firewall firewall = new Firewall(
                List.of(new Rule(Action.ALLOW, Protocol.TCP, ANYWHERE, PortRange.ANY, false, null),
                        deny("0.0.0.0/0", "22")),
                Action.DENY);

        firewall.evaluate(tcpTo("203.0.113.7", 22));

        assertEquals(1, firewall.hitCounts().get(0).hits());
        assertEquals(0, firewall.hitCounts().get(1).hits());
    }

    @Test
    void unmatchedTrafficIsCountedAgainstTheDefaultPolicy() {
        Firewall firewall = new Firewall(List.of(), Action.DENY);
        firewall.evaluate(tcpTo("10.0.0.5", 8080));

        assertEquals(1, firewall.defaultPolicyHits());
    }

    @Test
    void aSourceOverItsRateIsDeniedWhileAnotherSourceIsNot() {
        Rule limited = new Rule(Action.ALLOW, Protocol.TCP, ANYWHERE, PortRange.parse("80"),
                false, new RateLimit(3));
        Firewall firewall = new Firewall(List.of(limited), Action.DENY);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        // Same instant for every packet, so no refill happens and the burst of 3 is all there is.
        for (int i = 0; i < 3; i++) {
            assertEquals(Action.ALLOW, firewall.evaluate(packetAt("203.0.113.7", 80, now)));
        }
        assertEquals(Action.DENY, firewall.evaluate(packetAt("203.0.113.7", 80, now)));
        assertEquals(Action.ALLOW, firewall.evaluate(packetAt("10.0.0.5", 80, now)));
        assertEquals(1, firewall.rateLimited());
    }

    private static Packet packetAt(String srcIp, int dstPort, Instant at) {
        return new Packet(Ip.parse(srcIp), 51344, Ip.parse("93.184.216.34"), dstPort, Protocol.TCP, at);
    }

    @Test
    void replyIsAllowedOnlyAfterTheRequestWentOut() {
        Rule establishedOnly = new Rule(Action.ALLOW, Protocol.TCP, ANYWHERE, PortRange.ANY, true, null);
        Packet request = tcpTo("10.0.0.5", 443);

        Firewall coldStart = new Firewall(List.of(establishedOnly), Action.DENY);
        // Nothing has been sent yet, so the reply is a stranger.
        assertEquals(Action.DENY, coldStart.evaluate(request.reply(Instant.now())));

        Firewall warmed = new Firewall(List.of(establishedOnly, allow("0.0.0.0/0", "443")), Action.DENY);
        assertEquals(Action.ALLOW, warmed.evaluate(request));
        assertEquals(Action.ALLOW, warmed.evaluate(request.reply(Instant.now())));
    }
}
