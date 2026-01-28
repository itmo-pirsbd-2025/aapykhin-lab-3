package ru.aapykhin.lab3.stress;

import ru.aapykhin.lab3.list.HarrisLinkedList;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;

/**
 * JCStress tests for Harris Lock-Free Linked List.
 */
public class HarrisLinkedListStressTests {

    /**
     * Two threads try to add the same element - exactly one should succeed.
     */
    @JCStressTest
    @Outcome(id = "true, false", expect = Expect.ACCEPTABLE, desc = "Thread 1 added first")
    @Outcome(id = "false, true", expect = Expect.ACCEPTABLE, desc = "Thread 2 added first")
    @Outcome(id = "true, true", expect = Expect.FORBIDDEN, desc = "Both added - violation")
    @Outcome(id = "false, false", expect = Expect.FORBIDDEN, desc = "Neither added - lost update")
    @State
    public static class ConcurrentAddSameElement {
        private final HarrisLinkedList<Integer> list = new HarrisLinkedList<>();

        @Actor
        public void actor1(ZZ_Result r) {
            r.r1 = list.add(1);
        }

        @Actor
        public void actor2(ZZ_Result r) {
            r.r2 = list.add(1);
        }
    }

    /**
     * Two threads try to remove the same element - exactly one should succeed.
     */
    @JCStressTest
    @Outcome(id = "true, false", expect = Expect.ACCEPTABLE, desc = "Thread 1 removed")
    @Outcome(id = "false, true", expect = Expect.ACCEPTABLE, desc = "Thread 2 removed")
    @Outcome(id = "true, true", expect = Expect.FORBIDDEN, desc = "Both removed - double free")
    @Outcome(id = "false, false", expect = Expect.FORBIDDEN, desc = "Neither removed")
    @State
    public static class ConcurrentRemoveSameElement {
        private final HarrisLinkedList<Integer> list = new HarrisLinkedList<>();

        public ConcurrentRemoveSameElement() {
            list.add(1);
        }

        @Actor
        public void actor1(ZZ_Result r) {
            r.r1 = list.remove(1);
        }

        @Actor
        public void actor2(ZZ_Result r) {
            r.r2 = list.remove(1);
        }
    }

    /**
     * Concurrent adds of different elements - both should succeed.
     */
    @JCStressTest
    @Outcome(id = "true, true", expect = Expect.ACCEPTABLE, desc = "Both adds succeeded")
    @Outcome(id = "true, false", expect = Expect.FORBIDDEN, desc = "Add of 2 failed")
    @Outcome(id = "false, true", expect = Expect.FORBIDDEN, desc = "Add of 1 failed")
    @Outcome(id = "false, false", expect = Expect.FORBIDDEN, desc = "Both adds failed")
    @State
    public static class ConcurrentAddDifferent {
        private final HarrisLinkedList<Integer> list = new HarrisLinkedList<>();

        @Actor
        public void actor1(ZZ_Result r) {
            r.r1 = list.add(1);
        }

        @Actor
        public void actor2(ZZ_Result r) {
            r.r2 = list.add(2);
        }
    }
}
