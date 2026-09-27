package com.firewall;

// The five fields that identify one direction of one connection. Packet cannot be used
// as a map key because it also carries a timestamp, so two packets of the same
// connection would never be equal and the second one would never be found.
public record ConnectionKey(int srcIp, int srcPort, int dstIp, int dstPort, Protocol protocol) {

    public static ConnectionKey of(Packet packet) {
        return new ConnectionKey(packet.srcIp(), packet.srcPort(),
                packet.dstIp(), packet.dstPort(), packet.protocol());
    }
}
