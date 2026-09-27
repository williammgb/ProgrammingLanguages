package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PacketTest {

    @Test
    void replySwapsSenderAndReceiver() {
        Packet request = new Packet(Ip.parse("10.0.0.5"), 51344,
                Ip.parse("93.184.216.34"), 443, Protocol.TCP, Instant.now());

        Packet reply = request.reply(Instant.now());

        assertEquals(request.dstIp(), reply.srcIp());
        assertEquals(request.dstPort(), reply.srcPort());
        assertEquals(request.srcIp(), reply.dstIp());
        assertEquals(request.srcPort(), reply.dstPort());
        assertEquals(request.protocol(), reply.protocol());
    }
}
