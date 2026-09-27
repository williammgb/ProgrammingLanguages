package com.firewall;

// final class: cannot be inherited
public final class Ip {

    // Java has no top-level static class, so a private constructor is how you 
    // prevent object from being created (cannot be called outside this class)
    private Ip() {
    }

    public static int parse(String text) {
        // split takes regex; "." means match everything, so need to escape it "\."
        // and "\" itself is special and needs escaping, so: "\\."
        String[] octets = text.split("\\.");

        if (octets.length != 4) {
            throw new IllegalArgumentException("not an IPv4 address: " + text);
        }

        int result = 0;
        for (String octet : octets) {
            int value = Integer.parseInt(octet);
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("octet out of range in " + text + ": " + value);
            }
            // Every octet is converted to its bit-form and then pasted after each other
            // gives a 32-bit integer
            // << : bitshifting (move 8 bits to the left); | : bitwise OR (append to the new 8 bits)
            result = (result << 8) | value;
        }
        return result;
    }

    public static String format(int ip) {
        // converts integer value back to an IP string
        // >>> shifts zeros in from the left (i.e. moves bits to the right). 
        // 0xFF only collects the last 8 bits for each step
        return ((ip >>> 24) & 0xFF) + "."
                + ((ip >>> 16) & 0xFF) + "."
                + ((ip >>> 8) & 0xFF) + "."
                + (ip & 0xFF);
    }
}
