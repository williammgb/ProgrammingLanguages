package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CidrTest {

    // One method (containsHandlesBoundaries), many rows. Each row runs as its own test, 
    // so a failure names the exact address instead of just the method.
    @ParameterizedTest
    @CsvSource({
            "10.0.0.0/8,   10.0.0.0,        true",
            "10.0.0.0/8,   10.255.255.255,  true",
            "10.0.0.0/8,   9.255.255.255,   false",
            "10.0.0.0/8,   11.0.0.0,        false",
            "192.168.1.0/24, 192.168.1.255, true",
            "192.168.1.0/24, 192.168.2.0,   false",
            "0.0.0.0/0,    203.0.113.7,     true",
            "203.0.113.7/32, 203.0.113.7,   true",
            "203.0.113.7/32, 203.0.113.8,   false"
    })
    void containsHandlesBoundaries(String block, String address, boolean expected) {
        assertEquals(expected, Cidr.parse(block).contains(Ip.parse(address)));
    }

    @Test
    void hostBitsAreClearedSoEquivalentBlocksCompareEqual() {
        assertEquals(Cidr.parse("10.0.0.0/8"), Cidr.parse("10.0.0.5/8"));
    }

    @Test
    void slashZeroCoversAddressesWhoseIntIsNegative() {
        // 203.x sets bit 31, which is the case a naive signed comparison would get wrong.
        assertTrue(Cidr.parse("0.0.0.0/0").contains(Ip.parse("203.0.113.7")));
    }

    @Test
    void bareAddressMeansSlash32() {
        Cidr single = Cidr.parse("10.0.0.5");
        assertEquals(32, single.prefixLength());
        assertFalse(single.contains(Ip.parse("10.0.0.6")));
    }

    @Test
    void prefixLengthOutsideZeroToThirtyTwoIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Cidr.parse("10.0.0.0/33"));
    }
}
