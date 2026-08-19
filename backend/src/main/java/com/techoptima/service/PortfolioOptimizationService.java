package com.techoptima.service;

import com.techoptima.algorithm.graph.ApplicationDependencyGraph;
import com.techoptima.algorithm.graph.DependencyGraphBuilder;
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
import java.util.List;

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
         * 1. Build the authoritative ApplicationDependencyGraph from domain applications.
         */
        ApplicationDependencyGraph graph =
                DependencyGraphBuilder.build(applications);

        /*
         * 2. Run globally optimal dependency-aware 0/1 Knapsack optimization.
         * Evaluates dependency closure directly during selection.
         */
        KnapsackResult knapsackResult =
                ZeroOneKnapsackOptimizer.optimize(
                        applications,
                        budget
                );

        /*
         * 3. Validate dependency closure on the selected portfolio.
         */
        DependencyValidationResult dependencyResult =
                DependencyValidator.validate(
                        graph,
                        knapsackResult.getSelectedApplications()
                );

        /*
         * 4. Compute dependency-respecting execution sequence using the authoritative graph.
         */
        TopologicalSortResult topologyResult = null;
        if (dependencyResult.isValid()) {
            topologyResult =
                    TopologicalSorter.sort(
                            graph,
                            knapsackResult.getSelectedApplications()
                    );
        }

        return new OptimizationResult(
                budget,
                knapsackResult,
                dependencyResult,
                topologyResult
        );
    }
}
