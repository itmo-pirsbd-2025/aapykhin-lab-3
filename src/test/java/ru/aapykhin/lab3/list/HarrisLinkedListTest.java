package ru.aapykhin.lab3.list;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class HarrisLinkedListTest {

    private HarrisLinkedList<Integer> list;

    @BeforeEach
    void setUp() {
        list = new HarrisLinkedList<>();
    }

    @Test
    void testEmptyList() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.contains(1));
    }

    @Test
    void testAddAndContains() {
        assertTrue(list.add(1));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertTrue(list.contains(1));
        assertFalse(list.contains(2));
    }

    @Test
    void testAddDuplicate() {
        assertTrue(list.add(1));
        assertFalse(list.add(1));
        assertEquals(1, list.size());
    }

    @Test
    void testRemove() {
        list.add(1);
        list.add(2);
        assertTrue(list.remove(1));
        assertFalse(list.contains(1));
        assertTrue(list.contains(2));
        assertFalse(list.remove(1));
    }

    @Test
    void testSortedOrder() {
        list.add(5);
        list.add(2);
        list.add(8);
        list.add(1);
        assertEquals("[1, 2, 5, 8]", list.toString());
    }

    @Test
    void testConcurrentAdds() throws InterruptedException {
        int numThreads = 4;
        int elementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < elementsPerThread; i++) {
                        list.add(threadId * elementsPerThread + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(numThreads * elementsPerThread, list.size());
    }
}
