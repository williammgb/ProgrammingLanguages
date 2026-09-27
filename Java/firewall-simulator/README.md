# firewall-simulator

A packet-filtering firewall, simulated end to end in one process. A traffic generator
invents network packets, a rule engine loaded from a plain text file decides ALLOW or
DENY on each one, and a report at the end says what every rule actually did.

There are no real sockets anywhere. Every firewall idea worth learning — rule ordering,
subnet matching, connection tracking, rate limiting — is fully visible without them, and
the packets being ordinary Java objects is what makes the whole thing testable.

## Tools / stack

- **Java 21** (Temurin) — records, enums, text blocks, `java.time`
- **Maven** — build and test runner
- **JUnit 5** (Jupiter) — 47 tests, including parameterised and repeated ones
- No third-party libraries at runtime. The rule file parser is hand-written.

## What it does

Rules live in `firewall.rules`, one per line, read top to bottom. The first rule that
matches a packet decides it, and a packet no rule matches falls through to the default
policy at the bottom of the file.

```
# action  protocol  source        port          [established | N/s]
allow     tcp       any           any           established   # answers to connections we opened
allow     tcp       10.0.0.0/8    22            # SSH from our own network is fine
deny      tcp       any           22            # SSH from anywhere else, blocked
allow     tcp       10.0.0.0/8    30000-32000   # internal test servers live in this band
allow     tcp       any           443           # HTTPS, our web server listens on this port
allow     tcp       any           80            200/s   # HTTP redirect only, so no source needs more than this
deny      udp       any           any           # we run nothing on UDP

policy    deny
```

- **Source** is a subnet in CIDR form (`10.0.0.0/8`), a single address, or `any`.
- **Port** is one port, an inclusive range (`30000-32000`), or `any`.
- **`established`** matches only the answer to a connection this firewall already allowed
  out, so replies get back in without opening every high port to the internet.
- **`N/s`** caps how many packets per second one source may send through that rule.
- Anything the parser cannot read stops the program with the line number, rather than
  silently running a policy nobody wrote.

## Components

- **`Packet`, `Protocol`** — the 5-tuple (source address and port, destination address and
  port, protocol) plus a timestamp. Addresses are 32-bit `int`s, not strings.
- **`Ip`** — packs `10.0.0.5` into one `int` and back, so a subnet test is one bitwise AND.
- **`Cidr`, `PortRange`** — what a rule matches on. `Cidr` clears the host bits in its
  constructor, so `10.0.0.5/8` and `10.0.0.0/8` are the same value.
- **`Rule`, `Action`** — one rule and its verdict. `Rule.matches` answers only whether the
  rule is *about* a packet; it never decides anything.
- **`ConfigLoader`, `ConfigException`** — the hand-written parser for `firewall.rules`,
  reporting every error with its line number.
- **`Firewall`** — walks the rules in order, counts hits per rule, and holds the connection
  table and the rate-limit buckets.
- **`ConnectionTracker`, `ConnectionKey`** — remembers allowed connections so their replies
  can be recognised. Entries expire, and the table has a hard size cap, because it is fed
  by whoever is sending packets.
- **`TokenBucket`, `RateLimit`** — a bucket per source per rule, refilling at a steady rate.
- **`TrafficGenerator`** — synthetic traffic from a seeded random number generator, so a run
  is reproducible.
- **`Pipeline`** — one thread generating packets into a bounded queue, four filtering them
  out of it. The queue being bounded is what stops a fast generator exhausting memory.

## Setup

**1. Install a Java 21 JDK** (or newer), then verify:

```bash
java -version
```

**2. Install Maven** and verify it runs:

```bash
mvn -version
```

**3. Run it** from this folder:

```bash
mvn -q compile
java -cp target/classes com.firewall.Main
```

It must be started from this folder, since `firewall.rules` is read relative to wherever
`java` is launched.

**4. Run the tests:**

```bash
mvn test
```

## Output

```
50000 requests through 4 workers in 2313 ms
allowed 20277, denied 44482 (of which 4158 over a rate limit)

rule hits
       6035  ALLOW TCP 0.0.0.0/0 0-65535 established
       1679  ALLOW TCP 10.0.0.0/8 22
       1734  DENY TCP 0.0.0.0/0 22
         95  ALLOW TCP 10.0.0.0/8 30000-32000
       9856  ALLOW TCP 0.0.0.0/0 443
       6770  ALLOW TCP 0.0.0.0/0 80 200/s
      10114  DENY UDP 0.0.0.0/0 0-65535
      28476  policy DENY

top talkers
      12665  203.0.113.7
      12529  10.0.0.11
      12461  10.0.0.5
```

The hit counts are the useful part. A rule with **zero** hits is either dead weight or
shadowed by a broader rule above it, which is the single most common way a real firewall
config ends up not doing what its author believed.
