package com.firewall;

// One firewall rule: which traffic it describes, plus what to do with that traffic.
// rateLimit is null for a rule with no limit.
public record Rule(Action action, Protocol protocol, Cidr source, PortRange dstPort,
                   boolean requiresEstablished, RateLimit rateLimit) {

    // Says only whether this rule is about this packet. It does not allow or deny anything.
    // The caller passes in whether the connection is known, so the rule never reads the table.
    public boolean matches(Packet packet, boolean established) {
        // Different protocol, so this rule has nothing to say about this packet.
        if (packet.protocol() != protocol) {
            return false;
        }
        // This rule only covers answers to connections we opened.
        if (requiresEstablished && !established) {
            return false;
        }
        // Check if sent from outside the block this rule covers.
        if (!source.contains(packet.srcIp())) {
            return false;
        }
        // True if the packet's port falls inside the band this rule names.
        return dstPort.contains(packet.dstPort());
    }

    // Prints as the line that would produce this rule in firewall.rules.
    @Override
    public String toString() {
        return action + " " + protocol + " " + source + " " + dstPort
                + (requiresEstablished ? " established" : "")
                + (rateLimit != null ? " " + rateLimit : "");
    }
}
