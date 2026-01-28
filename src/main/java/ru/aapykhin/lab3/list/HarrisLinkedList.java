package ru.aapykhin.lab3.list;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Lock-free linked list implementation based on Harris algorithm.
 *
 * Reference: Timothy L. Harris, "A Pragmatic Implementation of Non-Blocking Linked-Lists"
 * https://www.cl.cam.ac.uk/research/srg/netos/papers/2001-caslists.pdf
 *
 * Key concepts:
 * 1. Logical deletion using marked references (AtomicMarkableReference)
 * 2. Two-phase deletion: first mark, then physically remove
 * 3. CAS (Compare-And-Swap) for all modifications
 * 4. Wait-free contains() operation
 * 5. Lock-free add() and remove() operations
 */
public class HarrisLinkedList<T extends Comparable<T>> {
    static class Node<T> {
        final T key;
        final AtomicMarkableReference<Node<T>> next;

        Node(T key, Node<T> next) {
            this.key = key;
            this.next = new AtomicMarkableReference<>(next, false);
        }
    }

    static class Window<T> {
        Node<T> pred;
        Node<T> curr;

        Window(Node<T> pred, Node<T> curr) {
            this.pred = pred;
            this.curr = curr;
        }
    }

    private final Node<T> head;
    private final Node<T> tail;

    public HarrisLinkedList() {
        this.tail = new Node<>(null, null);
        this.head = new Node<>(null, tail);
    }

    private Window<T> find(T key) {
        Node<T> pred, curr, succ;
        boolean[] marked = {false};
        boolean snip;

        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();

            while (true) {
                succ = curr.next.get(marked);

                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) {
                        continue retry;
                    }
                    curr = succ;
                    succ = curr.next.get(marked);
                }

                if (curr == tail || compare(curr.key, key) >= 0) {
                    return new Window<>(pred, curr);
                }

                pred = curr;
                curr = succ;
            }
        }
    }

    private int compare(T a, T b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    public boolean add(T key) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed");
        }

        while (true) {
            Window<T> window = find(key);
            Node<T> pred = window.pred;
            Node<T> curr = window.curr;

            if (curr != tail && compare(curr.key, key) == 0) {
                return false;
            }

            Node<T> newNode = new Node<>(key, curr);

            if (pred.next.compareAndSet(curr, newNode, false, false)) {
                return true;
            }
        }
    }

    public boolean remove(T key) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed");
        }

        boolean snip;
        while (true) {
            Window<T> window = find(key);
            Node<T> pred = window.pred;
            Node<T> curr = window.curr;

            if (curr == tail || compare(curr.key, key) != 0) {
                return false
            }

            Node<T> succ = curr.next.getReference();

            snip = curr.next.compareAndSet(succ, succ, false, true);
            if (!snip) {
                continue;
            }

            pred.next.compareAndSet(curr, succ, false, false);

            return true;
        }
    }

    public boolean contains(T key) {
        if (key == null) {
            throw new NullPointerException("Null keys are not allowed");
        }

        boolean[] marked = {false};
        Node<T> curr = head.next.getReference();

        while (curr != tail && compare(curr.key, key) < 0) {
            curr = curr.next.getReference();
        }

        curr.next.get(marked);
        return curr != tail && compare(curr.key, key) == 0 && !marked[0];
    }

    public boolean isEmpty() {
        Node<T> curr = head.next.getReference();
        boolean[] marked = {false};

        while (curr != tail) {
            curr.next.get(marked);
            if (!marked[0]) {
                return false;
            }
            curr = curr.next.getReference();
        }
        return true;
    }

    public int size() {
        int count = 0;
        boolean[] marked = {false};
        Node<T> curr = head.next.getReference();

        while (curr != tail) {
            curr.next.get(marked);
            if (!marked[0]) {
                count++;
            }
            curr = curr.next.getReference();
        }
        return count;
    }

    public void clear() {
        head.next.set(tail, false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean[] marked = {false};
        Node<T> curr = head.next.getReference();
        boolean first = true;

        while (curr != tail) {
            curr.next.get(marked);
            if (!marked[0]) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(curr.key);
                first = false;
            }
            curr = curr.next.getReference();
        }
        sb.append("]");
        return sb.toString();
    }
}
