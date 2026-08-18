package com.techoptima.algorithm.topology;

import com.techoptima.algorithm.priority.ApplicationPriorityComparator;
import com.techoptima.model.Application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class TopologicalSorter {

    private TopologicalSorter() {
    }

    /**
     * Produces a valid dependency-respecting upgrade order.
     *
     * Edge direction:
     * dependency -> dependent
     *
     * Kahn's algorithm:
     * Time  : O(V + E log V)
     * Space : O(V + E)
     *
     * The log V factor comes from using the existing application
     * priority rule when multiple nodes are ready.
     */
    public static TopologicalSortResult sort(
            Collection<Application> applications) {

        if (applications == null) {
            throw new IllegalArgumentException(
                    "applications cannot be null"
            );
        }

        Map<Long, Application> applicationsById =
                new LinkedHashMap<>();

        for (Application application : applications) {

            if (application == null) {
                throw new IllegalArgumentException(
                        "applications cannot contain null elements"
                );
            }

            long id = application.getApplicationId();

            if (applicationsById.put(id, application) != null) {
                throw new IllegalArgumentException(
                        "Duplicate applicationId: " + id
                );
            }
        }

        Map<Long, Set<Long>> outgoingEdges =
                new HashMap<>();

        Map<Long, Integer> indegree =
                new HashMap<>();

        for (Long id : applicationsById.keySet()) {
            outgoingEdges.put(id, new LinkedHashSet<>());
            indegree.put(id, 0);
        }

        for (Application application :
                applicationsById.values()) {

            long applicationId =
                    application.getApplicationId();

            for (Long dependencyId :
                    application.getDependencyApplicationIds()) {

                if (dependencyId == null) {
                    throw new IllegalArgumentException(
                            "Dependency ID cannot be null for application "
                                    + applicationId
                    );
                }

                if (!applicationsById.containsKey(dependencyId)) {
                    throw new IllegalArgumentException(
                            "Missing dependency "
                                    + dependencyId
                                    + " required by application "
                                    + applicationId
                    );
                }

                /*
                 * Avoid counting duplicate dependency entries
                 * more than once.
                 */
                if (outgoingEdges
                        .get(dependencyId)
                        .add(applicationId)) {

                    indegree.put(
                            applicationId,
                            indegree.get(applicationId) + 1
                    );
                }
            }
        }

        PriorityQueue<Application> readyQueue =
                new PriorityQueue<>(
                        ApplicationPriorityComparator.INSTANCE
                );

        for (Application application :
                applicationsById.values()) {

            if (indegree.get(
                    application.getApplicationId()
            ) == 0) {

                readyQueue.offer(application);
            }
        }

        List<Application> ordered =
                new ArrayList<>(applications.size());

        while (!readyQueue.isEmpty()) {

            Application current =
                    readyQueue.poll();

            long currentId =
                    current.getApplicationId();

            ordered.add(current);

            for (Long dependentId :
                    outgoingEdges.get(currentId)) {

                int newIndegree =
                        indegree.get(dependentId) - 1;

                indegree.put(
                        dependentId,
                        newIndegree
                );

                if (newIndegree == 0) {
                    readyQueue.offer(
                            applicationsById.get(dependentId)
                    );
                }
            }
        }

        if (ordered.size() == applicationsById.size()) {
            return new TopologicalSortResult(
                    true,
                    ordered,
                    List.of()
            );
        }

        List<Long> cyclicApplicationIds =
                new ArrayList<>();

        for (Map.Entry<Long, Integer> entry :
                indegree.entrySet()) {

            if (entry.getValue() > 0) {
                cyclicApplicationIds.add(
                        entry.getKey()
                );
            }
        }

        return new TopologicalSortResult(
                false,
                ordered,
                cyclicApplicationIds
        );
    }
}