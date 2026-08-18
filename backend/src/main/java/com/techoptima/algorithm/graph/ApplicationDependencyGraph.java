package com.techoptima.algorithm.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the directed dependency graph of enterprise applications.
 *
 * Edge direction:
 * dependency -> dependent application
 *
 * Example:
 * CRM -> Analytics
 *
 * means Analytics depends on CRM.
 */
public final class ApplicationDependencyGraph {

    private final Map<Long, Set<Long>> adjacencyList;
    private final Set<Long> knownApplicationIds;

    public ApplicationDependencyGraph() {
        this.adjacencyList = new LinkedHashMap<>();
        this.knownApplicationIds = new LinkedHashSet<>();
    }

    /**
     * Registers an actual application as a graph node.
     */
    public void addApplication(long applicationId) {
        validateApplicationId(applicationId);

        knownApplicationIds.add(applicationId);
        adjacencyList.computeIfAbsent(applicationId, ignored -> new LinkedHashSet<>());
    }

    /**
     * Adds a dependency edge:
     *
     * dependencyApplicationId -> applicationId
     *
     * Example:
     * CRM(1) -> Analytics(3)
     */
    public void addDependency(long applicationId, long dependencyApplicationId) {
        validateApplicationId(applicationId);
        validateApplicationId(dependencyApplicationId);

        adjacencyList.computeIfAbsent(dependencyApplicationId, ignored -> new LinkedHashSet<>());
        adjacencyList.computeIfAbsent(applicationId, ignored -> new LinkedHashSet<>());

        adjacencyList
                .get(dependencyApplicationId)
                .add(applicationId);
    }

    /**
     * Returns the applications that directly depend on the given application.
     */
    public List<Long> getDependents(long applicationId) {
        validateApplicationId(applicationId);

        Set<Long> dependents = adjacencyList.get(applicationId);

        if (dependents == null) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(dependents));
    }

    /**
     * Returns all known application IDs.
     *
     * This contains only actual applications supplied to the graph builder.
     */
    public Set<Long> getKnownApplicationIds() {
        return Collections.unmodifiableSet(knownApplicationIds);
    }

    /**
     * Returns all graph node IDs.
     *
     * This may also contain referenced dependency IDs that are not yet
     * registered as actual applications. This is intentionally preserved
     * for later dependency validation.
     */
    public Set<Long> getNodeIds() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    /**
     * Returns a safe read-only view of the adjacency list.
     */
    public Map<Long, List<Long>> getAdjacencyList() {
        Map<Long, List<Long>> result = new LinkedHashMap<>();

        for (Map.Entry<Long, Set<Long>> entry : adjacencyList.entrySet()) {
            result.put(
                    entry.getKey(),
                    Collections.unmodifiableList(new ArrayList<>(entry.getValue()))
            );
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns true when the application ID is known as an actual application.
     */
    public boolean containsApplication(long applicationId) {
        validateApplicationId(applicationId);
        return knownApplicationIds.contains(applicationId);
    }

    /**
     * Returns the number of unique graph nodes.
     */
    public int size() {
        return adjacencyList.size();
    }

    /**
     * Returns the number of unique directed edges.
     */
    public int edgeCount() {
        int count = 0;

        for (Set<Long> dependents : adjacencyList.values()) {
            count += dependents.size();
        }

        return count;
    }

    private void validateApplicationId(long applicationId) {
        if (applicationId <= 0) {
            throw new IllegalArgumentException(
                    "applicationId must be greater than 0"
            );
        }
    }
}