package com.firewall;

// The ports a rule covers, low and high included. A single port is a range of one
public record PortRange(int low, int high) {

    public static final PortRange ANY = new PortRange(0, 65535);

    public PortRange {
        if (low < 0 || high > 65535) {
            throw new IllegalArgumentException("ports must be between 0 and 65535, got " + low + "-" + high);
        }
        if (low > high) {
            throw new IllegalArgumentException("range runs backwards: " + low + "-" + high);
        }
    }

    public static PortRange parse(String text) {
        int dash = text.indexOf('-');

        // No dash means one port, stored as a range whose ends are the same.
        if (dash < 0) {
            int port = Integer.parseInt(text);
            return new PortRange(port, port);
        }
        return new PortRange(
                Integer.parseInt(text.substring(0, dash)),
                Integer.parseInt(text.substring(dash + 1)));
    }

    public boolean contains(int port) {
        return port >= low && port <= high;
    }

    // Prints the way the rule file writes it.
    @Override
    public String toString() {
        return low == high ? String.valueOf(low) : low + "-" + high;
    }
}
