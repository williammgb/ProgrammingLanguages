package com.firewall;

// Determines a CIDR network block of addresses like 10.0.0.0/8:
// the network number, plus how many leading bits of it a packet has to match.
public record Cidr(int network, int prefixLength) {

    public Cidr {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("prefix length must be 0 to 32, got " + prefixLength);
        }
        // Filters to the subnet range the prefix covers, so 10.0.0.5/8 and 10.0.0.0/8
        // become the same value (10.x.x.x) and compare equal.
        // & is bitwise AND, that only keeps the bits in the mask
        network = network & maskFor(prefixLength);
    }

    public static Cidr parse(String text) {
        int slash = text.indexOf('/');

        // No slash means one single address, which is a block with all 32 bits fixed.
        if (slash < 0) {
            return new Cidr(Ip.parse(text), 32);
        }
        return new Cidr(
                Ip.parse(text.substring(0, slash)),
                Integer.parseInt(text.substring(slash + 1)));
    }

    public boolean contains(int ip) {
        // Blank out the bits the prefix ignores; whatever is left has to be identical.
        return (ip & maskFor(prefixLength)) == network;
    }

    // The generated toString would print network=167772160, which no one can read.
    @Override
    public String toString() {
        return Ip.format(network) + "/" + prefixLength;
    }

    private static int maskFor(int prefixLength) {
        // -1 is all 32 bits set to 1;
        // << (32 - n) means shifting 32-n bits to the left (so n ones, then 32-n zeros)
        // n = 0 means all 32 bits are set to 0
        // only the 1s are the bits that must be compared in this mask
        return prefixLength == 0 ? 0 : -1 << (32 - prefixLength);
    }
}
