package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PortRangeTest {

    @Test
    void bothEndsOfARangeAreIncluded() {
        PortRange range = PortRange.parse("30000-32000");
        assertTrue(range.contains(30000));
        assertTrue(range.contains(32000));
        assertFalse(range.contains(29999));
        assertFalse(range.contains(32001));
    }

    @Test
    void singlePortBecomesARangeOfOne() {
        assertEquals(new PortRange(443, 443), PortRange.parse("443"));
    }

    @Test
    void anyCoversTheWholePortSpace() {
        assertTrue(PortRange.ANY.contains(0));
        assertTrue(PortRange.ANY.contains(65535));
    }

    @Test
    void backwardsAndOutOfRangeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PortRange.parse("2000-1000"));
        assertThrows(IllegalArgumentException.class, () -> PortRange.parse("70000"));
    }
}
