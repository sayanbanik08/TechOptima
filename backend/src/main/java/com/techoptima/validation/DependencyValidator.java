package com.techoptima.validation;

import com.techoptima.model.Application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyValidator {

    private DependencyValidator() {
    }

    /**
     * Checks whether every dependency required by a selected
     * application is also selected.
     *
     * Time complexity: O(V + E)
     * Space complexity: O(V + E)
     */
    public static DependencyValidationResult validate(
            Collection<Application> selectedApplications) {

        if (selectedApplications == null) {
            throw new IllegalArgumentException(
                    "selectedApplications cannot be null"
            );
        }

        Set<Long> selectedIds =
                new HashSet<>();

        for (Application application : selectedApplications) {

            if (application == null) {
                throw new IllegalArgumentException(
                        "selectedApplications cannot contain null"
                );
            }

            long applicationId =
                    application.getApplicationId();

            if (!selectedIds.add(applicationId)) {
                throw new IllegalArgumentException(
                        "Duplicate selected applicationId: "
                                + applicationId
                );
            }
        }

        Map<Long, List<Long>> missingByApplication =
                new LinkedHashMap<>();

        Set<Long> uniqueMissingDependencies =
                new LinkedHashSet<>();

        for (Application application : selectedApplications) {

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

                /*
                 * A dependency is satisfied only when it is part
                 * of the selected application set.
                 */
                if (!selectedIds.contains(dependencyId)) {

                    missingByApplication
                            .computeIfAbsent(
                                    applicationId,
                                    ignored -> new ArrayList<>()
                            )
                            .add(dependencyId);

                    uniqueMissingDependencies.add(
                            dependencyId
                    );
                }
            }
        }

        return new DependencyValidationResult(
                missingByApplication.isEmpty(),
                new ArrayList<>(
                        uniqueMissingDependencies
                ),
                missingByApplication
        );
    }
}