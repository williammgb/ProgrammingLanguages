package com.firewall;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

// Runs the firewall on several threads at once: one thread makes packets, several
// filter them, and a queue sits in between.
public class Pipeline {

    // A packet no rule will ever see, handed to a worker to mean "there is no more
    // work". Compared with ==, so it is the object itself that carries the meaning.
    private static final Packet POISON =
            new Packet(0, 0, 0, 0, Protocol.ICMP, Instant.EPOCH);

    private final Firewall firewall;
    private final int workerCount;
    private final BlockingQueue<Packet> queue;

    // With ordinary long, 2 threads can overwrite. AtomicLong is thread-safe
    private final AtomicLong allowed = new AtomicLong();
    private final AtomicLong denied = new AtomicLong();

    // LongAdder, not AtomicLong: it spreads writes over several cells, which is faster
    // when many threads hit the same counter and slower to read back.
    private final Map<Integer, LongAdder> packetsBySource = new ConcurrentHashMap<>();

    public Pipeline(Firewall firewall, int workerCount, int queueCapacity) {
        this.firewall = firewall;
        this.workerCount = workerCount;
        // Bounded on purpose: an unbounded queue turns a slow consumer into an
        // out-of-memory crash instead of a producer that simply has to wait.
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
    }

    public void run(TrafficGenerator generator, int packetCount) throws InterruptedException {
        ExecutorService workers = Executors.newFixedThreadPool(workerCount);
        CountDownLatch finished = new CountDownLatch(workerCount);

        for (int i = 0; i < workerCount; i++) {
            // submit() gives task to workers; in this case: each worker picks a packet
            // from the queue, puts it through firewall, and gets the next packet
            workers.submit(() -> {
                try {
                    consume();
                } finally {
                    // Counts down even if consume() throws, so a crashed worker
                    // cannot leave the main thread waiting forever.
                    finished.countDown();
                }
            });
        }

        for (int i = 0; i < packetCount; i++) {
            // if queue is full, it blocks, and generator has to wait for queue to free up before sending next packet
            // this is called backpressure: the generator is forced down to the speed of the filters.
            queue.put(generator.next());
        }

        // One pill per worker, because each worker takes exactly one and then stops.
        for (int i = 0; i < workerCount; i++) {
            queue.put(POISON);
        }

        finished.await(); // wait until all workers are done
        workers.shutdown();
        workers.awaitTermination(10, TimeUnit.SECONDS);
    }

    public long allowed() {
        return allowed.get();
    }

    public long denied() {
        return denied.get();
    }

    // A plain snapshot, taken after the run, so the caller never sees a LongAdder
    // that is still being written to.
    public Map<Integer, Long> packetsBySource() {
        Map<Integer, Long> snapshot = new HashMap<>();
        packetsBySource.forEach((ip, adder) -> snapshot.put(ip, adder.sum()));
        return snapshot;
    }

    private void consume() {
        try {
            while (true) {
                Packet packet = queue.take();
                if (packet == POISON) {
                    return;
                }

                // Counted before the verdict, so a blocked sender still shows up.
                packetsBySource.computeIfAbsent(packet.srcIp(), ip -> new LongAdder()).increment();

                Action verdict = firewall.evaluate(packet);
                count(verdict);

                // The reply is handled by the same worker as its request, so the
                // firewall can never see an answer before the question.
                if (verdict == Action.ALLOW) {
                    count(firewall.evaluate(packet.reply(Instant.now())));
                }
            }
        } catch (InterruptedException e) {
            // Catching InterruptedException clears the interrupt flag, so it has to
            // be set again or the thread pool never learns it was asked to stop.
            Thread.currentThread().interrupt();
        }
    }

    private void count(Action verdict) {
        if (verdict == Action.ALLOW) {
            allowed.incrementAndGet();
        } else {
            denied.incrementAndGet();
        }
    }
}
