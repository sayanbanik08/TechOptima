package com.techoptima.algorithm.priority;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationPriorityQueueTest {

    private Application application(
            long id,
            String name,
            String cost,
            int benefit,
            Criticality criticality) {

        return new Application(
                id,
                name,
                new BigDecimal(cost),
                benefit,
                criticality,
                Department.OPERATIONS,
                List.of()
        );
    }

    @Test
    void shouldPrioritizeCriticalOverLowerCriticality() {

        Application low =
                application(
                        1L,
                        "LOW",
                        "10.00",
                        100,
                        Criticality.LOW
                );

        Application high =
                application(
                        2L,
                        "HIGH",
                        "10.00",
                        50,
                        Criticality.HIGH
                );

        Application critical =
                application(
                        3L,
                        "CRITICAL",
                        "10.00",
                        10,
                        Criticality.CRITICAL
                );

        ApplicationPriorityQueue queue =
                ApplicationPriorityQueue.from(
                        List.of(low, high, critical)
                );

        assertEquals(
                critical,
                queue.poll()
        );

        assertEquals(
                high,
                queue.poll()
        );

        assertEquals(
                low,
                queue.poll()
        );
    }

    @Test
    void shouldUseBusinessBenefitWhenCriticalityMatches() {

        Application lowerBenefit =
                application(
                        1L,
                        "A",
                        "10.00",
                        60,
                        Criticality.HIGH
                );

        Application higherBenefit =
                application(
                        2L,
                        "B",
                        "20.00",
                        90,
                        Criticality.HIGH
                );

        ApplicationPriorityQueue queue =
                ApplicationPriorityQueue.from(
                        List.of(
                                lowerBenefit,
                                higherBenefit
                        )
                );

        assertEquals(
                higherBenefit,
                queue.poll()
        );

        assertEquals(
                lowerBenefit,
                queue.poll()
        );
    }

    @Test
    void shouldUseLowerCostAsTieBreaker() {

        Application expensive =
                application(
                        1L,
                        "Expensive",
                        "30.00",
                        80,
                        Criticality.HIGH
                );

        Application cheap =
                application(
                        2L,
                        "Cheap",
                        "20.00",
                        80,
                        Criticality.HIGH
                );

        ApplicationPriorityQueue queue =
                ApplicationPriorityQueue.from(
                        List.of(
                                expensive,
                                cheap
                        )
                );

        assertEquals(
                cheap,
                queue.poll()
        );

        assertEquals(
                expensive,
                queue.poll()
        );
    }

    @Test
    void shouldUseApplicationIdAsFinalTieBreaker() {

        Application first =
                application(
                        1L,
                        "First",
                        "20.00",
                        80,
                        Criticality.HIGH
                );

        Application second =
                application(
                        2L,
                        "Second",
                        "20.00",
                        80,
                        Criticality.HIGH
                );

        ApplicationPriorityQueue queue =
                ApplicationPriorityQueue.from(
                        List.of(
                                second,
                                first
                        )
                );

        assertEquals(first, queue.poll());
        assertEquals(second, queue.poll());
    }

    @Test
    void shouldDrainInPriorityOrder() {

        Application low =
                application(
                        1L,
                        "LOW",
                        "10.00",
                        100,
                        Criticality.LOW
                );

        Application critical =
                application(
                        2L,
                        "CRITICAL",
                        "10.00",
                        10,
                        Criticality.CRITICAL
                );

        Application high =
                application(
                        3L,
                        "HIGH",
                        "10.00",
                        50,
                        Criticality.HIGH
                );

        ApplicationPriorityQueue queue =
                ApplicationPriorityQueue.from(
                        List.of(low, critical, high)
                );

        assertEquals(
                List.of(critical, high, low),
                queue.drainInPriorityOrder()
        );

        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldHandleEmptyQueue() {

        ApplicationPriorityQueue queue =
                new ApplicationPriorityQueue();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
        assertNull(queue.poll());
    }

    @Test
    void shouldRejectNullApplication() {

        ApplicationPriorityQueue queue =
                new ApplicationPriorityQueue();

        assertThrows(
                IllegalArgumentException.class,
                () -> queue.add(null)
        );
    }

    @Test
    void shouldRejectNullCollection() {

        ApplicationPriorityQueue queue =
                new ApplicationPriorityQueue();

        assertThrows(
                IllegalArgumentException.class,
                () -> queue.addAll(null)
        );
    }
}