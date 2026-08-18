package com.techoptima.service;

import com.techoptima.algorithm.knapsack.KnapsackResult;
import com.techoptima.algorithm.topology.TopologicalSortResult;
import com.techoptima.model.TransformationBudget;
import com.techoptima.validation.DependencyValidationResult;

public final class OptimizationResult {

    private final TransformationBudget budget;
    private final KnapsackResult knapsackResult;
    private final DependencyValidationResult dependencyValidationResult;
    private final TopologicalSortResult topologicalSortResult;

    public OptimizationResult(
            TransformationBudget budget,
            KnapsackResult knapsackResult,
            DependencyValidationResult dependencyValidationResult,
            TopologicalSortResult topologicalSortResult) {

        if (budget == null) {
            throw new IllegalArgumentException("budget cannot be null");
        }

        if (knapsackResult == null) {
            throw new IllegalArgumentException(
                    "knapsackResult cannot be null"
            );
        }

        if (dependencyValidationResult == null) {
            throw new IllegalArgumentException(
                    "dependencyValidationResult cannot be null"
            );
        }

        if (dependencyValidationResult.isValid()
                && topologicalSortResult == null) {
            throw new IllegalArgumentException(
                    "topologicalSortResult is required when dependencies are valid"
            );
        }

        this.budget = budget;
        this.knapsackResult = knapsackResult;
        this.dependencyValidationResult =
                dependencyValidationResult;
        this.topologicalSortResult =
                topologicalSortResult;
    }

    public TransformationBudget getBudget() {
        return budget;
    }

    public KnapsackResult getKnapsackResult() {
        return knapsackResult;
    }

    public DependencyValidationResult getDependencyValidationResult() {
        return dependencyValidationResult;
    }

    public TopologicalSortResult getTopologicalSortResult() {
        return topologicalSortResult;
    }

    public boolean isSuccessful() {
        return dependencyValidationResult.isValid()
                && topologicalSortResult != null
                && topologicalSortResult.isValid();
    }
}