package com.techoptima.algorithm.priority;

import com.techoptima.model.Application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

public final class ApplicationPriorityQueue {

    private final PriorityQueue<Application> queue;

    public ApplicationPriorityQueue() {
        this.queue =
                new PriorityQueue<>(
                        ApplicationPriorityComparator.INSTANCE
                );
    }

    public void add(Application application) {

        if (application == null) {
            throw new IllegalArgumentException(
                    "application cannot be null"
            );
        }

        queue.offer(application);
    }

    public void addAll(
            Collection<Application> applications) {

        if (applications == null) {
            throw new IllegalArgumentException(
                    "applications cannot be null"
            );
        }

        for (Application application : applications) {
            add(application);
        }
    }

    public Application poll() {
        return queue.poll();
    }

    public Application peek() {
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    /**
     * Returns applications in priority order.
     *
     * The internal PriorityQueue remains encapsulated.
     */
    public List<Application> drainInPriorityOrder() {

        List<Application> ordered =
                new ArrayList<>(queue.size());

        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }

        return ordered;
    }

    /**
     * Creates a new priority queue from the supplied collection.
     */
    public static ApplicationPriorityQueue from(
            Collection<Application> applications) {

        ApplicationPriorityQueue priorityQueue =
                new ApplicationPriorityQueue();

        priorityQueue.addAll(applications);

        return priorityQueue;
    }
}