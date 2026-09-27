package com.firewall;

// Thrown when the rule file contains something we cannot turn into a rule.
public class ConfigException extends RuntimeException {

    public ConfigException(int lineNumber, String message) {
        super("firewall.rules line " + lineNumber + ": " + message);
    }
}
