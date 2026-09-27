package com.firewall;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Reads the rule file and builds a Firewall from it. Anything it cannot parse
// stops the program, because thats better than running without a policy.
public class ConfigLoader {
    // This method can cause IOException, and code must handle it.
    // In this class we do not handle it, so it is passed on sto the code
    // that calls load().
    public static Firewall load(Path path) throws IOException {
        List<Rule> rules = new ArrayList<>();

        // Stays null until a policy line is found, which is how we detect a file that has none.
        Action defaultPolicy = null;

        List<String> lines = Files.readAllLines(path);

        for (int i = 0; i < lines.size(); i++) {
            // Skip header line; thereafter: go to next line
            int lineNumber = i + 1;

            String line = lines.get(i);

            // Cut everything from the first # onward, so a rule can carry a comment
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            // "\s+" means one or more spaces or tabs
            String[] parts = line.split("\\s+");

            // .equals compares the text; == would compare object identity and be false here.
            if (parts[0].equals("policy")) {
                if (parts.length != 2) {
                    throw new ConfigException(lineNumber, "expected 'policy allow' or 'policy deny', got: " + line);
                }
                defaultPolicy = parseAction(parts[1], lineNumber);
                continue;
            }

            if (parts.length < 4 || parts.length > 5) {
                throw new ConfigException(lineNumber,
                        "expected 'action protocol source port [established | N/s]', got: " + line);
            }

            // The optional fifth column is either the word 'established' or a rate like 50/s.
            boolean requiresEstablished = false;
            RateLimit rateLimit = null;
            if (parts.length == 5) {
                if (parts[4].equals("established")) {
                    requiresEstablished = true;
                } else {
                    rateLimit = parseRateLimit(parts[4], lineNumber);
                }
            }

            rules.add(new Rule(
                    parseAction(parts[0], lineNumber),
                    parseProtocol(parts[1], lineNumber),
                    parseCidr(parts[2], lineNumber),
                    parsePortRange(parts[3], lineNumber),
                    requiresEstablished,
                    rateLimit));
        }

        // Without a policy line the firewall has no action for unmatched traffic, so refuse to start.
        if (defaultPolicy == null) {
            throw new ConfigException(lines.size(), "file has no 'policy allow' or 'policy deny' line");
        }

        return new Firewall(rules, defaultPolicy);
    }

    private static Action parseAction(String token, int lineNumber) {
        try {
            // valueOf matches the enum constant name exactly, so "deny" has to be upper-cased first.
            return Action.valueOf(token.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigException(lineNumber, "unknown action '" + token + "', expected allow or deny");
        }
    }

    private static Protocol parseProtocol(String token, int lineNumber) {
        try {
            return Protocol.valueOf(token.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigException(lineNumber, "unknown protocol '" + token + "', expected tcp, udp or icmp");
        }
    }

    private static Cidr parseCidr(String token, int lineNumber) {
        // /0 covers every address, so "any" in the source column is not a special case,
        // just a shorter way to write the block that already means everything.
        if (token.equals("any")) {
            return new Cidr(0, 0);
        }
        try {
            return Cidr.parse(token);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(lineNumber, "bad source '" + token + "': " + e.getMessage());
        }
    }

    private static RateLimit parseRateLimit(String token, int lineNumber) {
        try {
            return RateLimit.parse(token);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(lineNumber,
                    "fifth column must be 'established' or a rate like 50/s, got '" + token + "'");
        }
    }

    private static PortRange parsePortRange(String token, int lineNumber) {
        if (token.equals("any")) {
            return PortRange.ANY;
        }
        try {
            return PortRange.parse(token);
        } catch (IllegalArgumentException e) {
            // NumberFormatException is a subclass of IllegalArgumentException, so this one
            // catch covers both a non-number and a range PortRange itself rejected.
            throw new ConfigException(lineNumber,
                    "port must be a number, a low-high range, or 'any', got '" + token + "'");
        }
    }
}
