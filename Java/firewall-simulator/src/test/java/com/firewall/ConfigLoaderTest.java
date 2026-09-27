package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

    // JUnit creates this directory before each test and deletes it after, so the
    // tests never touch the real firewall.rules.
    @TempDir
    Path tempDir;

    private Path rulesFile(String contents) throws IOException {
        Path path = tempDir.resolve("test.rules");
        Files.writeString(path, contents);
        return path;
    }

    @Test
    void commentsAndBlankLinesAreIgnored() throws IOException {
        Firewall firewall = ConfigLoader.load(rulesFile("""
                # a whole-line comment

                allow tcp any 443   # a trailing comment
                policy deny
                """));

        Packet https = new Packet(Ip.parse("10.0.0.5"), 51344,
                Ip.parse("93.184.216.34"), 443, Protocol.TCP, Instant.now());

        assertEquals(Action.ALLOW, firewall.evaluate(https));
    }

    @Test
    void aBadActionReportsTheLineItIsOn() throws IOException {
        Path path = rulesFile("""
                allow tcp any 443
                alow  tcp any 80
                policy deny
                """);

        ConfigException thrown = assertThrows(ConfigException.class, () -> ConfigLoader.load(path));
        assertTrue(thrown.getMessage().contains("line 2"), thrown.getMessage());
    }

    @Test
    void aFileWithNoPolicyLineIsRefused() throws IOException {
        Path path = rulesFile("allow tcp any 443\n");
        assertThrows(ConfigException.class, () -> ConfigLoader.load(path));
    }
}
