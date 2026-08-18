package com.techoptima.service;

import com.techoptima.algorithm.knapsack.KnapsackResult;
import com.techoptima.algorithm.knapsack.ZeroOneKnapsackOptimizer;
import com.techoptima.algorithm.topology.TopologicalSortResult;
import com.techoptima.algorithm.topology.TopologicalSorter;
import com.techoptima.model.Application;
import com.techoptima.model.TransformationBudget;
import com.techoptima.repository.ApplicationRepository;
import com.techoptima.repository.TransformationBudgetRepository;
import com.techoptima.validation.DependencyValidationResult;
import com.techoptima.validation.DependencyValidator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PortfolioOptimizationService {

    private final ApplicationRepository applicationRepository;
    private final TransformationBudgetRepository budgetRepository;

    public PortfolioOptimizationService() {
        this(
                new ApplicationRepository(),
                new TransformationBudgetRepository()
        );
    }

    public PortfolioOptimizationService(
            ApplicationRepository applicationRepository,
            TransformationBudgetRepository budgetRepository) {

        if (applicationRepository == null) {
            throw new IllegalArgumentException(
                    "applicationRepository cannot be null"
            );
        }

        if (budgetRepository == null) {
            throw new IllegalArgumentException(
                    "budgetRepository cannot be null"
            );
        }

        this.applicationRepository = applicationRepository;
        this.budgetRepository = budgetRepository;
    }

    public OptimizationResult optimize()
            throws SQLException {

        TransformationBudget budget =
                budgetRepository.findLatest();

        if (budget == null) {
            throw new IllegalStateException(
                    "No transformation budget exists in the database"
            );
        }

        List<Application> applications =
                applicationRepository.findAll();

        if (applications.isEmpty()) {
            throw new IllegalStateException(
                    "No applications exist in the database"
            );
        }

        /*
         * Applications that have already been proven to produce an
         * invalid dependency selection are excluded from subsequent
         * optimization attempts.
         */
        Set<Long> excludedApplicationIds =
                new HashSet<>();

        while (true) {

            List<Application> candidates =
                    filterApplications(
                            applications,
                            excludedApplicationIds
                    );

            /*
             * Nothing remains that can be selected.
             */
            if (candidates.isEmpty()) {

                KnapsackResult emptyResult =
                        new KnapsackResult(
                                List.of(),
                                java.math.BigDecimal.ZERO.setScale(2),
                                0
                        );

                DependencyValidationResult dependencyResult =
                        new DependencyValidationResult(
                                true,
                                List.of(),
                                java.util.Map.of()
                        );

                return new OptimizationResult(
                        budget,
                        emptyResult,
                        dependencyResult,
                        null
                );
            }

            KnapsackResult knapsackResult =
                    ZeroOneKnapsackOptimizer.optimize(
                            candidates,
                            budget
                    );

            DependencyValidationResult dependencyResult =
                    DependencyValidator.validate(
                            knapsackResult.getSelectedApplications()
                    );

            /*
             * Normal case:
             * the knapsack result already satisfies every dependency.
             */
            if (dependencyResult.isValid()) {

                TopologicalSortResult topologyResult =
                        TopologicalSorter.sort(
                                knapsackResult.getSelectedApplications()
                        );

                return new OptimizationResult(
                        budget,
                        knapsackResult,
                        dependencyResult,
                        topologyResult
                );
            }

            /*
             * Dependency-invalid applications cannot remain in the
             * candidate pool for the next optimization attempt.
             *
             * Example:
             *
             * Analytics -> CRM
             *
             * If Analytics is selected but CRM is not selected,
             * Analytics is excluded and the optimizer is executed again.
             */
            Set<Long> invalidSelectedIds =
                    new HashSet<>(
                            dependencyResult
                                    .getMissingDependenciesByApplication()
                                    .keySet()
                    );

            /*
             * Safety guard. Every invalid iteration must remove at least
             * one application. This prevents an accidental infinite loop
             * if the validation implementation changes in the future.
             */
            if (invalidSelectedIds.isEmpty()) {

                return new OptimizationResult(
                        budget,
                        knapsackResult,
                        dependencyResult,
                        null
                );
            }

            excludedApplicationIds.addAll(
                    invalidSelectedIds
            );
        }
    }

    private static List<Application> filterApplications(
            List<Application> applications,
            Set<Long> excludedApplicationIds) {

        List<Application> candidates =
                new ArrayList<>();

        for (Application application : applications) {

            if (!excludedApplicationIds.contains(
                    application.getApplicationId())) {

                candidates.add(application);
            }
        }

        return candidates;
    }
}
