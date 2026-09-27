package com.firewall;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public class Main {

    private static final int PACKET_COUNT = 50_000;
    private static final int WORKER_COUNT = 4;

    // InterruptedException is checked too, so the same rule as IOException applies.
    public static void main(String[] args) throws IOException, InterruptedException {

        // Relative to whatever directory you run java from, not to where the .class files live.
        Firewall firewall = ConfigLoader.load(Path.of("firewall.rules"));

        // Change this number and you get completely different traffic; keep it and
        // every run is identical.
        TrafficGenerator generator = new TrafficGenerator(42);

        // The queue is far smaller than the packet count on purpose, so the generator
        // spends most of the run waiting for room rather than racing ahead.
        Pipeline pipeline = new Pipeline(firewall, WORKER_COUNT, 1_000);

        long startedAt = System.nanoTime();
        pipeline.run(generator, PACKET_COUNT);
        long millis = (System.nanoTime() - startedAt) / 1_000_000;

        System.out.println(PACKET_COUNT + " requests through " + WORKER_COUNT + " workers in " + millis + " ms");
        System.out.println("allowed " + pipeline.allowed() + ", denied " + pipeline.denied()
                + " (of which " + firewall.rateLimited() + " over a rate limit)");

        printRuleHits(firewall);
        printTopTalkers(pipeline);
    }

    private static void printRuleHits(Firewall firewall) {
        System.out.println("\nrule hits");
        for (RuleHits entry : firewall.hitCounts()) {
            // A rule nothing ever matched is either shadowed by one above it or dead
            // weight, and both are worth knowing about.
            String flag = entry.hits() == 0 ? "   <- never matched" : "";
            System.out.printf("  %9d  %s%s%n", entry.hits(), entry.rule(), flag);
        }
        System.out.printf("  %9d  policy %s%n", firewall.defaultPolicyHits(), firewall.defaultPolicy());
    }

    private static void printTopTalkers(Pipeline pipeline) {
        System.out.println("\ntop talkers");
        pipeline.packetsBySource().entrySet().stream()
                // comparingByValue sorts smallest first, so it has to be reversed.
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> System.out.printf("  %9d  %s%n", e.getValue(), Ip.format(e.getKey())));
    }
}
