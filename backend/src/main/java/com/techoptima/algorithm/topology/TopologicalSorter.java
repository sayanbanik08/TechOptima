package com.techoptima.algorithm.topology;

import com.techoptima.algorithm.graph.ApplicationDependencyGraph;
import com.techoptima.algorithm.graph.DependencyGraphBuilder;
import com.techoptima.algorithm.priority.ApplicationPriorityComparator;
import com.techoptima.model.Application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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
     * Produces a valid dependency-respecting upgrade order using the supplied authoritative graph.
     *
     * Edge direction:
     * dependency -> dependent
     *
     * Kahn's algorithm:
     * Time  : O(V + E log V)
     * Space : O(V + E)
     */
    public static TopologicalSortResult sort(
            ApplicationDependencyGraph graph,
            Collection<Application> applications) {

        if (graph == null) {
            throw new IllegalArgumentException(
                    "graph cannot be null"
            );
        }

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
            }

            for (Long dependentId : graph.getDependents(applicationId)) {
                if (applicationsById.containsKey(dependentId)) {
                    if (outgoingEdges.get(applicationId).add(dependentId)) {
                        indegree.put(
                                dependentId,
                                indegree.get(dependentId) + 1
                        );
                    }
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
                findCyclicApplicationIds(
                        outgoingEdges,
                        applicationsById.keySet()
                );

        return new TopologicalSortResult(
                false,
                ordered,
                cyclicApplicationIds
        );
    }

    /**
     * Produces a valid dependency-respecting upgrade order by building the graph from applications.
     */
    public static TopologicalSortResult sort(
            Collection<Application> applications) {

        if (applications == null) {
            throw new IllegalArgumentException(
                    "applications cannot be null"
            );
        }

        ApplicationDependencyGraph graph =
                DependencyGraphBuilder.build(applications);

        return sort(graph, applications);
    }

    /**
     * Identifies only applications that are part of a cycle. Kahn's remaining
     * non-zero indegrees also include applications merely blocked by a cycle,
     * so they cannot be used directly for user-facing cycle reporting.
     */
    private static List<Long> findCyclicApplicationIds(
            Map<Long, Set<Long>> outgoingEdges,
            Collection<Long> applicationIds) {

        Map<Long, Integer> discoveryIndexes =
                new HashMap<>();
        Map<Long, Integer> lowLinks =
                new HashMap<>();
        Deque<Long> stack = new ArrayDeque<>();
        Set<Long> onStack = new LinkedHashSet<>();
        Set<Long> cyclicIds = new LinkedHashSet<>();
        int[] nextIndex = {0};

        for (Long applicationId : applicationIds) {
            if (!discoveryIndexes.containsKey(applicationId)) {
                findCycleComponents(
                        applicationId,
                        outgoingEdges,
                        discoveryIndexes,
                        lowLinks,
                        stack,
                        onStack,
                        cyclicIds,
                        nextIndex
                );
            }
        }

        List<Long> orderedCyclicIds = new ArrayList<>();
        for (Long applicationId : applicationIds) {
            if (cyclicIds.contains(applicationId)) {
                orderedCyclicIds.add(applicationId);
            }
        }

        return orderedCyclicIds;
    }

    /**
     * Tarjan's strongly connected components algorithm. Its O(V + E) pass is
     * only required after Kahn's algorithm has already found a cycle.
     */
    private static void findCycleComponents(
            Long applicationId,
            Map<Long, Set<Long>> outgoingEdges,
            Map<Long, Integer> discoveryIndexes,
            Map<Long, Integer> lowLinks,
            Deque<Long> stack,
            Set<Long> onStack,
            Set<Long> cyclicIds,
            int[] nextIndex) {

        int index = nextIndex[0]++;
        discoveryIndexes.put(applicationId, index);
        lowLinks.put(applicationId, index);
        stack.push(applicationId);
        onStack.add(applicationId);

        for (Long dependentId : outgoingEdges.get(applicationId)) {
            if (!discoveryIndexes.containsKey(dependentId)) {
                findCycleComponents(
                        dependentId,
                        outgoingEdges,
                        discoveryIndexes,
                        lowLinks,
                        stack,
                        onStack,
                        cyclicIds,
                        nextIndex
                );
                lowLinks.put(
                        applicationId,
                        Math.min(
                                lowLinks.get(applicationId),
                                lowLinks.get(dependentId)
                        )
                );
            } else if (onStack.contains(dependentId)) {
                lowLinks.put(
                        applicationId,
                        Math.min(
                                lowLinks.get(applicationId),
                                discoveryIndexes.get(dependentId)
                        )
                );
            }
        }

        if (!lowLinks.get(applicationId).equals(
                discoveryIndexes.get(applicationId))) {
            return;
        }

        List<Long> component = new ArrayList<>();
        Long componentMember;
        do {
            componentMember = stack.pop();
            onStack.remove(componentMember);
            component.add(componentMember);
        } while (!componentMember.equals(applicationId));

        if (component.size() > 1
                || outgoingEdges.get(applicationId).contains(applicationId)) {
            cyclicIds.addAll(component);
        }
    }
}
