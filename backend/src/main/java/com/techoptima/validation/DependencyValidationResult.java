package com.techoptima.validation;

import java.util.List;
import java.util.Map;

public final class DependencyValidationResult {

    private final boolean valid;
    private final List<Long> missingDependencyApplicationIds;
    private final Map<Long, List<Long>> missingDependenciesByApplication;

    public DependencyValidationResult(
            boolean valid,
            List<Long> missingDependencyApplicationIds,
            Map<Long, List<Long>> missingDependenciesByApplication) {

        if (missingDependencyApplicationIds == null) {
            throw new IllegalArgumentException(
                    "missingDependencyApplicationIds cannot be null"
            );
        }

        if (missingDependenciesByApplication == null) {
            throw new IllegalArgumentException(
                    "missingDependenciesByApplication cannot be null"
            );
        }

        this.valid = valid;
        this.missingDependencyApplicationIds =
                List.copyOf(missingDependencyApplicationIds);
        this.missingDependenciesByApplication =
                Map.copyOf(missingDependenciesByApplication);
    }

    public boolean isValid() {
        return valid;
    }

    public List<Long> getMissingDependencyApplicationIds() {
        return missingDependencyApplicationIds;
    }

    public Map<Long, List<Long>> getMissingDependenciesByApplication() {
        return missingDependenciesByApplication;
    }

    @Override
    public String toString() {
        return "DependencyValidationResult{" +
                "valid=" + valid +
                ", missingDependencyApplicationIds=" +
                missingDependencyApplicationIds +
                ", missingDependenciesByApplication=" +
                missingDependenciesByApplication +
                '}';
    }
}