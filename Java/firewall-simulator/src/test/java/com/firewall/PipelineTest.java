package com.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class PipelineTest {

    // A firewall that denies everything, so no reply is ever generated and the
    // expected totals are exact no matter how the threads interleave.
    private static Firewall denyEverything() {
        return new Firewall(List.of(), Action.DENY);
    }

    // Repeated because a concurrency bug that shows up once in ten runs is the
    // normal case, and one green run proves nothing.
    @RepeatedTest(10)
    void noPacketIsLostOrCountedTwice() throws InterruptedException {
        Pipeline pipeline = new Pipeline(denyEverything(), 4, 16);
        pipeline.run(new TrafficGenerator(1), 5_000);

        assertEquals(0, pipeline.allowed());
        assertEquals(5_000, pipeline.denied());
    }

    @Test
    void aQueueSmallerThanTheWorkerCountStillFinishes() throws InterruptedException {
        // Capacity 1 means the producer blocks almost immediately, which is the case
        // where a missing poison pill would deadlock instead of failing loudly.
        Pipeline pipeline = new Pipeline(denyEverything(), 8, 1);
        pipeline.run(new TrafficGenerator(2), 100);

        assertEquals(100, pipeline.denied());
    }
}
