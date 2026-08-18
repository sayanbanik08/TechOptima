package com.techoptima.algorithm.topology;

import com.techoptima.model.Application;

import java.util.List;

public final class TopologicalSortResult {

    private final boolean valid;
    private final List<Application> orderedApplications;
    private final List<Long> cyclicApplicationIds;

    public TopologicalSortResult(
            boolean valid,
            List<Application> orderedApplications,
            List<Long> cyclicApplicationIds) {

        if (orderedApplications == null) {
            throw new IllegalArgumentException(
                    "orderedApplications cannot be null"
            );
        }

        if (cyclicApplicationIds == null) {
            throw new IllegalArgumentException(
                    "cyclicApplicationIds cannot be null"
            );
        }

        this.valid = valid;
        this.orderedApplications = List.copyOf(orderedApplications);
        this.cyclicApplicationIds = List.copyOf(cyclicApplicationIds);
    }

    public boolean isValid() {
        return valid;
    }

    public List<Application> getOrderedApplications() {
        return orderedApplications;
    }

    public List<Long> getCyclicApplicationIds() {
        return cyclicApplicationIds;
    }

    @Override
    public String toString() {
        return "TopologicalSortResult{" +
                "valid=" + valid +
                ", orderedApplications=" + orderedApplications +
                ", cyclicApplicationIds=" + cyclicApplicationIds +
                '}';
    }
}