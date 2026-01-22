package ru.aapykhin.lab3.benchmark;

import ru.aapykhin.lab3.list.HarrisLinkedList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks for Harris Lock-Free Linked List.
 *
 * Compares performance against:
 * - ConcurrentSkipListSet (Java standard library)
 * - Collections.synchronizedSet (lock-based baseline)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class HarrisLinkedListBenchmark {

    @Param({"1", "4", "8"})
    private int threads;

    @Param({"1000"})
    private int initialSize;

    @Param({"50"})
    private int readPercentage;

    private HarrisLinkedList<Integer> harrisList;
    private ConcurrentSkipListSet<Integer> skipListSet;
    private Set<Integer> synchronizedSet;

    private int maxKey;

    @Setup(Level.Iteration)
    public void setup() {
        harrisList = new HarrisLinkedList<>();
        skipListSet = new ConcurrentSkipListSet<>();
        synchronizedSet = Collections.synchronizedSet(new java.util.TreeSet<>());

        maxKey = initialSize * 2;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < initialSize; i++) {
            int key = random.nextInt(maxKey);
            harrisList.add(key);
            skipListSet.add(key);
            synchronizedSet.add(key);
        }
    }

    // ==================== Harris Lock-Free List ====================

    @Benchmark
    @Group("harris")
    @GroupThreads(1)
    public void harrisOperations(Blackhole bh) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int key = random.nextInt(maxKey);
        int op = random.nextInt(100);

        if (op < readPercentage) {
            bh.consume(harrisList.contains(key));
        } else if (op < readPercentage + (100 - readPercentage) / 2) {
            bh.consume(harrisList.add(key));
        } else {
            bh.consume(harrisList.remove(key));
        }
    }

    // ==================== ConcurrentSkipListSet ====================

    @Benchmark
    @Group("skiplist")
    @GroupThreads(1)
    public void skipListOperations(Blackhole bh) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int key = random.nextInt(maxKey);
        int op = random.nextInt(100);

        if (op < readPercentage) {
            bh.consume(skipListSet.contains(key));
        } else if (op < readPercentage + (100 - readPercentage) / 2) {
            bh.consume(skipListSet.add(key));
        } else {
            bh.consume(skipListSet.remove(key));
        }
    }

    // ==================== Synchronized Set ====================

    @Benchmark
    @Group("synchronized")
    @GroupThreads(1)
    public void synchronizedOperations(Blackhole bh) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int key = random.nextInt(maxKey);
        int op = random.nextInt(100);

        if (op < readPercentage) {
            bh.consume(synchronizedSet.contains(key));
        } else if (op < readPercentage + (100 - readPercentage) / 2) {
            bh.consume(synchronizedSet.add(key));
        } else {
            bh.consume(synchronizedSet.remove(key));
        }
    }

    // ==================== Pure Operations Benchmarks ====================

    @State(Scope.Thread)
    public static class ThreadState {
        int counter = 0;

        int nextKey(int max) {
            return ThreadLocalRandom.current().nextInt(max);
        }
    }

    @Benchmark
    public boolean harrisContains(ThreadState state) {
        return harrisList.contains(state.nextKey(maxKey));
    }

    @Benchmark
    public boolean harrisAdd(ThreadState state) {
        return harrisList.add(state.nextKey(maxKey));
    }

    @Benchmark
    public boolean harrisRemove(ThreadState state) {
        return harrisList.remove(state.nextKey(maxKey));
    }

    @Benchmark
    public boolean skipListContains(ThreadState state) {
        return skipListSet.contains(state.nextKey(maxKey));
    }

    @Benchmark
    public boolean skipListAdd(ThreadState state) {
        return skipListSet.add(state.nextKey(maxKey));
    }

    @Benchmark
    public boolean skipListRemove(ThreadState state) {
        return skipListSet.remove(state.nextKey(maxKey));
    }

    // ==================== Contention Benchmarks ====================
    @State(Scope.Benchmark)
    public static class HighContentionState {
        HarrisLinkedList<Integer> list = new HarrisLinkedList<>();
        ConcurrentSkipListSet<Integer> skipList = new ConcurrentSkipListSet<>();
        final int keyRange = 100; // Small range = high contention

        @Setup(Level.Iteration)
        public void setup() {
            list = new HarrisLinkedList<>();
            skipList = new ConcurrentSkipListSet<>();
            for (int i = 0; i < 50; i++) {
                list.add(i);
                skipList.add(i);
            }
        }
    }

    @Benchmark
    @Group("contention_harris")
    @GroupThreads(4)
    public void harrisHighContention(HighContentionState state, Blackhole bh) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int key = random.nextInt(state.keyRange);
        int op = random.nextInt(3);

        switch (op) {
            case 0 -> bh.consume(state.list.contains(key));
            case 1 -> bh.consume(state.list.add(key));
            default -> bh.consume(state.list.remove(key));
        }
    }

    @Benchmark
    @Group("contention_skiplist")
    @GroupThreads(4)
    public void skipListHighContention(HighContentionState state, Blackhole bh) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int key = random.nextInt(state.keyRange);
        int op = random.nextInt(3);

        switch (op) {
            case 0 -> bh.consume(state.skipList.contains(key));
            case 1 -> bh.consume(state.skipList.add(key));
            default -> bh.consume(state.skipList.remove(key));
        }
    }

    // ==================== Main ====================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(HarrisLinkedListBenchmark.class.getSimpleName())
                .threads(4)
                .build();

        new Runner(opt).run();
    }
}
