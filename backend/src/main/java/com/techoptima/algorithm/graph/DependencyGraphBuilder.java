package com.techoptima.algorithm.graph;

import com.techoptima.model.Application;

import java.util.Collection;

/**
 * Builds an application dependency graph from the domain model.
 */
public final class DependencyGraphBuilder {

    private DependencyGraphBuilder() {
    }

    /**
     * Builds a directed dependency graph from the supplied applications.
     *
     * For:
     * Analytics -> dependency CRM
     *
     * the graph stores:
     * CRM -> Analytics
     *
     * Referenced dependency IDs that are not present in the supplied
     * applications are preserved as graph nodes so that later stages
     * can detect invalid dependencies.
     */
    public static ApplicationDependencyGraph build(
            Collection<Application> applications) {

        if (applications == null) {
            throw new IllegalArgumentException("applications cannot be null");
        }

        ApplicationDependencyGraph graph = new ApplicationDependencyGraph();

        for (Application application : applications) {
            if (application == null) {
                throw new IllegalArgumentException(
                        "applications cannot contain null elements"
                );
            }

            graph.addApplication(application.getApplicationId());
        }

        for (Application application : applications) {
            long applicationId = application.getApplicationId();

            for (Long dependencyApplicationId
                    : application.getDependencyApplicationIds()) {

                if (dependencyApplicationId == null) {
                    throw new IllegalArgumentException(
                            "dependencyApplicationIds cannot contain null values"
                    );
                }

                graph.addDependency(
                        applicationId,
                        dependencyApplicationId
                );
            }
        }

        return graph;
    }
}