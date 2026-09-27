package com.firewall;

// Only the protocols this simulator generates. ICMP carries no ports (only works between devices, not software)
public enum Protocol {
    TCP,
    UDP,
    ICMP
}
